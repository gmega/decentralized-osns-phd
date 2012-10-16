package it.unitn.disi.simulator.concurrent;

import it.unitn.disi.utils.CallbackThreadPoolExecutor;
import it.unitn.disi.utils.IExecutorCallback;
import it.unitn.disi.utils.logging.IProgressTracker;
import it.unitn.disi.utils.logging.Progress;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

/**
 * Helper class for running blocks of tasks and getting the results for
 * completed tasks first.
 * 
 * @author giuliano
 */
public class TaskExecutor {

	private static final Logger fLogger = Logger.getLogger(TaskExecutor.class);

	private volatile CallbackThreadPoolExecutor<Object> fExecutor;

	private final int fCores;

	private final LinkedBlockingQueue<Object> fReady = new LinkedBlockingQueue<Object>(
			10);

	private final Semaphore fSema;

	private final AtomicInteger fTasks = new AtomicInteger();

	private final AtomicInteger fConsumed = new AtomicInteger();

	private volatile IProgressTracker fTracker;

	public TaskExecutor(int cores) {
		this(cores, Integer.MAX_VALUE);
	}

	public TaskExecutor(int cores, int maxQueuedTasks) {
		createExecutor(cores);
		fSema = new Semaphore(maxQueuedTasks);
		fCores = cores;
	}

	public void createExecutor(int cores) {
		fExecutor = new CallbackThreadPoolExecutor<Object>(cores > 0 ? cores
				: Runtime.getRuntime().availableProcessors(),
				new IExecutorCallback<Object>() {
					@Override
					public void taskFailed(Future<Object> task, Throwable ex) {
						fTracker.tick();
						fLogger.error("Task ended with an error.", ex);
						queue(ex);
					}

					@Override
					public synchronized void taskDone(Object result) {
						fTracker.tick();
						queue(result);
					}

					private void queue(Object result) {
						try {
							fSema.release();
							fReady.offer(result, Long.MAX_VALUE, TimeUnit.DAYS);
						} catch (InterruptedException ex) {
							ex.printStackTrace();
						}
					}
				});
	}

	public synchronized void start(String task, int taskBlock) {
		fTasks.set(taskBlock);
		fConsumed.set(taskBlock);
		fTracker = Progress.synchronizedTracker(Progress.newTracker(task,
				taskBlock));
		fTracker.startTask();
	}

	public void submit(Callable<? extends Object> callable)
			throws InterruptedException {
		if (fTasks.decrementAndGet() < 0) {
			throw new IllegalStateException("Task block is complete.");
		}

		fSema.acquire();
		fExecutor.submit(callable);
	}

	public Object consume() throws InterruptedException {
		if (fConsumed.decrementAndGet() < 0) {
			throw new IllegalStateException("All tasks have been consumed.");
		}
		return fReady.poll(Long.MAX_VALUE, TimeUnit.DAYS);
	}

	public synchronized void cancelBatch() {
		// Stops accepting new tasks.
		fTasks.set(0);
		fConsumed.set(0);

		// If this aborts with an interrupted exception, there might be
		// unwanted tasks joining the queue later. It is a freakshow condition,
		// so I won't bother with it for now, but will check.
		try {
			// Waits for all submitted tasks to terminate.
			shutdown(true);
		} catch (InterruptedException ex) {
			throw new RuntimeException(
					"Shutdown interrupted, state might be inconsistent.");
		} finally {
			fReady.clear();
			createExecutor(fCores);
		}
	}

	public void shutdown(boolean wait) throws InterruptedException {
		fExecutor.shutdown();
		if (wait) {
			fExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		}
	}

}
