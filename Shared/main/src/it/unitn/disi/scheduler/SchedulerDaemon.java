package it.unitn.disi.scheduler;

import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.collections.NullOutputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

import org.apache.log4j.Logger;

/**
 * The {@link SchedulerDaemon} can queue and dispatch jobs, redirecting their
 * stdin/stderr to a file, as well as reading another file to direct to the
 * standard input. The number of simultaneous jobs is configurable, so that as
 * many cores as possible can be kept busy. <BR>
 * 
 * @author giuliano
 * @see ClientAPI
 * @see Client
 */
public class SchedulerDaemon implements IDaemon, Iterable<ProcessEntry> {

	// --------------------------------------------------------------------------
	// Statics and constants.
	// --------------------------------------------------------------------------

	/**
	 * Logger instance.
	 */
	private static final Logger fLogger = Logger
			.getLogger(SchedulerDaemon.class);

	/**
	 * Name of this object in the RMI registry.
	 */
	public static final String SCHEDULER_DAEMON = "scheduler daemon";

	/**
	 * Polling interval for the process garbage collector (ms).
	 */
	public static final int GC_POLLING_INTERVAL = 1000;

	// --------------------------------------------------------------------------
	// State.
	// --------------------------------------------------------------------------

	/**
	 * Table of running processes.
	 */
	private final ConcurrentHashMap<Integer, ProcessEntry> fProcessTable = new ConcurrentHashMap<Integer, ProcessEntry>();

	/**
	 * The dispatching component.
	 */
	private volatile ProcessDispatcher fDispatcher;

	/**
	 * The process reaper.
	 */
	private volatile GarbageCollector fCollector;

	/**
	 * Semaphore for controlling the maximum number of simultaneously running
	 * processes.
	 */
	private final Semaphore fSemaphore;

	/**
	 * {@link CountDownLatch} used to implement the {@link #join()} operation.
	 */
	private final CountDownLatch fRunState;

	// --------------------------------------------------------------------------

	/**
	 * Constructs a new {@link SchedulerDaemon}.
	 * 
	 * @param cores
	 *            the maximum number of processes that the scheduler should
	 *            allow to run simultaneously.
	 */
	public SchedulerDaemon(int cores) {
		fSemaphore = new Semaphore(cores);
		fRunState = new CountDownLatch(1);
	}

	// --------------------------------------------------------------------------

	/**
	 * Blocks until the {@link SchedulerDaemon} is shut down.
	 * 
	 * @throws InterruptedException
	 *             if the calling thread is interrupted.
	 */
	public void join() throws InterruptedException {
		fRunState.await();
	}

	// --------------------------------------------------------------------------

	/**
	 * Starts the server.
	 */
	public synchronized void start() throws RemoteException {
		fLogger.info("Initializing server.");

		if (fDispatcher != null) {
			throw new RemoteException("Server already running.");
		}

		fDispatcher = new ProcessDispatcher(this);
		fCollector = new GarbageCollector(this, GC_POLLING_INTERVAL);

		fDispatcher.start();
		fCollector.start();

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					SchedulerDaemon.this.shutdown();
				} catch (RemoteException ex) {
					fLogger.error("Clean shutdown failed.", ex);
				}
			}
		});
	}

	// --------------------------------------------------------------------------
	// IDaemon interface.
	// --------------------------------------------------------------------------

	@Override
	public synchronized void shutdown() throws RemoteException {
		// Server has already been shut down.
		if (fRunState.getCount() == 0) {
			return;
		}

		fLogger.info("Initiate orderly shutdown.");

		// Waits for the dispatcher to finish. We do this to
		// ensure that no further jobs will be dispatched
		// from this point on.
		fDispatcher.interrupt();
		fLogger.info("Waiting for the process dispatcher.");
		noInterruptJoin(fDispatcher);

		// Kills all processes.
		fLogger.info("Terminating all processes...");
		this.killall();
		// And waits for them to terminate.
		while (fProcessTable.size() > 0) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException ex) {
				// Don't care, it's my thread.
			}
		}
		fLogger.info("done.");

		// We know that the garbage collector is idle by now,
		// so we can safely stop it.
		fCollector.interrupt();
		fLogger.info("Waiting for the garbage collector.");
		noInterruptJoin(fCollector);

		// Done. Releases everyone waiting on join.
		fLogger.info("Shutdown sequence complete.");
		fRunState.countDown();
	}

	// --------------------------------------------------------------------------

	@Override
	public void submit(CommandDescriptor command) throws RemoteException {
		fDispatcher.submit(command);
	}

	// --------------------------------------------------------------------------

	@Override
	public synchronized void kill(int pid) throws RemoteException {
		ProcessEntry entry = fProcessTable.get(pid);
		if (entry == null) {
			throw new NoSuchElementException(Integer.toString(pid));
		}
		entry.process.destroy();
	}

	// --------------------------------------------------------------------------

	@Override
	public synchronized void killall() throws RemoteException {
		for (ProcessEntry entry : fProcessTable.values()) {
			entry.process.destroy();
		}
	}

	// --------------------------------------------------------------------------

	@Override
	public synchronized List<ProcessDescriptor> list() throws RemoteException {
		ArrayList<ProcessDescriptor> list = new ArrayList<ProcessDescriptor>();
		for (ProcessEntry entry : fProcessTable.values()) {
			list.add(entry.descriptor);
		}
		return list;
	}

	// --------------------------------------------------------------------------

	@Override
	public Iterator<ProcessEntry> iterator() {
		return fProcessTable.values().iterator();
	}

	// --------------------------------------------------------------------------
	// Private helpers.
	// --------------------------------------------------------------------------

	private void noInterruptJoin(Thread t) {
		while (true) {
			try {
				t.join();
				break;
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				continue;
			}
		}
	}

	// --------------------------------------------------------------------------
	// Callbacks.
	// --------------------------------------------------------------------------

	public void acquireCore() throws InterruptedException {
		fSemaphore.acquire();
	}

	// --------------------------------------------------------------------------

	public void releaseCore() {
		fSemaphore.release();
	}

	// --------------------------------------------------------------------------

	public void processStarted(ProcessEntry entry) {
		fProcessTable.put(entry.descriptor.pid, entry);
	}

	// --------------------------------------------------------------------------

	public void launchFailed(CommandDescriptor next, Exception ex) {
		fLogger.error("Launch for " + next.command + " failed. Reason: "
				+ ex.getMessage());
	}

	// --------------------------------------------------------------------------

	public void processDone(ProcessEntry entry, int value, long time) {
		fLogger.info("Process " + entry.descriptor.pid + ": terminated.");

		// Waits for the stream processors to stop.
		try {
			if (entry.input != null) {
				entry.input.join();
			}

			if (entry.output != null) {
				entry.output.join();
			}
		} catch (InterruptedException ex) {
			// Shouldn't happen, as we are not calling interrupt in these
			// threads.
		}
		fLogger.info("Process " + entry.descriptor.pid
				+ ": pipe streams terminated. Completion time:" + time
				+ " (approximately).");

		// Removes process from process table.
		fProcessTable.remove(entry.descriptor.pid);

		// Clears the way for the next process to be launched.
		releaseCore();
	}

}

/**
 * The {@link ProcessDispatcher} is the component responsible for actually
 * queueing commands and launching processes.
 * 
 * @author giuliano
 */
class ProcessDispatcher extends Thread {

	private static final Logger logger = Logger
			.getLogger(ProcessDispatcher.class);

	private final LinkedBlockingQueue<CommandDescriptor> fQueued = new LinkedBlockingQueue<CommandDescriptor>();

	private final SchedulerDaemon fParent;

	private int fPidCounter = 0;

	public ProcessDispatcher(SchedulerDaemon parent) {
		super("Process Dispatcher");
		fParent = parent;
	}

	@Override
	public void run() {
		while (!Thread.interrupted()) {
			CommandDescriptor next = null;
			try {
				// Waits forever, potentially.
				next = fQueued.poll(Long.MAX_VALUE, TimeUnit.DAYS);
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				continue;
			}

			try {
				fParent.acquireCore();
			} catch (InterruptedException ex) {
				break;
			}

			logger.info("Executing command \"" + next.commandString() + "\".");
			if (!launch(next)) {
				fParent.releaseCore();
			}
		}
	}

	private boolean launch(CommandDescriptor next) {
		InputStream input = null;
		OutputStream output = null;
		
		try {
			// First tries to open inputs and outputs.
			File pwd = checkPwd(next);
			input = openIStream(next);
			output = openOStream(next);

			// Executes the command.
			Process process = startProcess(next, pwd);

			int pid = fPidCounter++;

			// Starts stream processors, and constructs the process entry.
			ProcessEntry entry = new ProcessEntry(new ProcessDescriptor(next,
					pid), process, start(pipe(input, process.getOutputStream(),
					pid)), start(pipe(process.getInputStream(), output, pid)));

			// Notifies the parent.
			fParent.processStarted(entry);
			return true;
		} catch (Exception ex) {
			MiscUtils.safeClose(input, false);
			MiscUtils.safeClose(output, false);
			fParent.launchFailed(next, ex);
			return false;
		}
	}

	private Thread pipe(InputStream input, OutputStream output, int pid) {
		if (input == null) {
			return null;
		}
		return new Thread(new StreamPipe(input, output, pid));
	}

	private Thread start(Thread thread) {
		if (thread != null) {
			thread.start();
		}
		return thread;
	}

	private Process startProcess(CommandDescriptor next, File pwd)
			throws IOException {
		ProcessBuilder builder = new ProcessBuilder(next.command);
		builder.directory(pwd);
		builder.redirectErrorStream(true);
		Process process = builder.start();
		return process;
	}

	private OutputStream openOStream(CommandDescriptor command)
			throws FileNotFoundException, IOException {
		String output = command.output;

		if (output.equals(CommandDescriptor.NONE)) {
			return new NullOutputStream();
		}

		OutputStream fOut = new FileOutputStream(new File(output));

		if (command.compressOutput) {
			fOut = new GZIPOutputStream(fOut);
		}

		return fOut;
	}

	private InputStream openIStream(CommandDescriptor command)
			throws FileNotFoundException {
		String input = command.input;

		if (input.equals(CommandDescriptor.NONE)) {
			return null;
		}

		return new FileInputStream(new File(input));
	}

	private File checkPwd(CommandDescriptor command)
			throws FileNotFoundException {
		if (command.pwd.equals(CommandDescriptor.NONE)) {
			return null;
		}

		File pwd = new File(command.pwd);
		if (pwd.exists()) {
			return pwd;
		} else {
			throw new FileNotFoundException("Working directory " + command.pwd
					+ " was not found.");
		}
	}

	public void submit(CommandDescriptor descriptor) {
		logger.info("Queueing command \"" + descriptor.commandString() + "\".");
		fQueued.add(descriptor);
	}
}

class GarbageCollector extends Thread {

	private final int fPollingInterval;

	private final SchedulerDaemon fParent;

	public GarbageCollector(SchedulerDaemon parent, int pollingInterval) {
		super("Garbage Collector");
		fParent = parent;
		fPollingInterval = pollingInterval;
	}

	@Override
	public synchronized void run() {
		while (!Thread.interrupted()) {
			for (ProcessEntry entry : fParent) {
				try {
					int value = entry.process.exitValue();
					// Quite loose, as we have no idea for how long the process
					// has already been dead. Only way to fix this would be to
					// not use polling, but instead launch a thread per process.
					long time = System.currentTimeMillis() - entry.startTime;
					fParent.processDone(entry, value, time);
				} catch (IllegalThreadStateException ex) {
					// Swallows, means process hasn't terminated.
				}
			}

			try {
				Thread.currentThread().wait(fPollingInterval);
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}
	}
}

class StreamPipe implements Runnable {

	private static final Logger logger = Logger.getLogger(StreamPipe.class);

	public static final int DEFAULT_BUFFER_SIZE = 16384;

	private final InputStream fIStream;

	private final OutputStream fOStream;

	private final int fBufferSize;

	private final int fPid;

	public StreamPipe(InputStream iStream, OutputStream oStream, int pid) {
		this(iStream, oStream, pid, DEFAULT_BUFFER_SIZE);
	}

	public StreamPipe(InputStream iStream, OutputStream oStream, int pid,
			int buffer) {
		fIStream = iStream;
		fOStream = oStream;
		fBufferSize = buffer;
		fPid = pid;
	}

	@Override
	public void run() {
		try {
			run0();
		} catch (IOException ex) {
			logger.warn("Broken pipe [" + fPid + "]: " + ex.getMessage());
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		} finally {
			MiscUtils.safeClose(fIStream, false);
			MiscUtils.safeClose(fOStream, false);
		}
	}

	private void run0() throws IOException, InterruptedException {
		byte[] buffer = new byte[fBufferSize];
		while (true) {
			int read = fIStream.read(buffer);
			if (read < 0) {
				logger.info("End-of-stream reached.");
				break;
			} else if (read > 0) {
				fOStream.write(buffer, 0, read);
			}
		}
	}

}

class ProcessEntry {
	public final ProcessDescriptor descriptor;

	public final Process process;

	public final Thread input;

	public final Thread output;

	public final long startTime;

	public volatile boolean terminating = false;

	public ProcessEntry(ProcessDescriptor descriptor, Process process,
			Thread input, Thread output) {
		this.descriptor = descriptor;
		this.process = process;
		this.startTime = System.currentTimeMillis();
		this.input = input;
		this.output = output;
	}
}
