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
 * Helper class for queueing and running blocks of tasks over several cores, and
 * getting the results for completed tasks first.
 * 
 * @author giuliano
 */
public class TaskExecutor {

	private static final Logger fLogger = Logger.getLogger(TaskExecutor.class);

	private final AtomicInteger fActiveTasks = new AtomicInteger();

	private volatile CallbackThreadPoolExecutor<Object> fExecutor;

	private volatile LinkedBlockingQueue<Object> fReady = new LinkedBlockingQueue<Object>();

	private volatile Semaphore fSema;

	private final int fCores;

	private int fBlockSize;

	private int fTasks;

	private int fConsumed;

	private volatile IProgressTracker fTracker;

	private int fMaxQueuedTasks;

	public TaskExecutor(int cores) {
		this(cores, Integer.MAX_VALUE);
	}

	/**
	 * Constructs a new {@link TaskExecutor}.
	 * 
	 * @param cores
	 *            the maximum number of cores to use for running submitted
	 *            tasks.
	 * 
	 * @param maxQueuedTasks
	 *            the maximum number of tasks allowed to be in the queue before
	 *            submissions starts to block.
	 */
	public TaskExecutor(int cores, int maxQueuedTasks) {
		fMaxQueuedTasks = maxQueuedTasks;
		fCores = cores;
		createExecutor(cores);
		semaphoreAndQueue();
	}

	public void createExecutor(int cores) {
		fExecutor = new CallbackThreadPoolExecutor<Object>(cores > 0 ? cores
				: Runtime.getRuntime().availableProcessors(),
				new IExecutorCallback<Object>() {
					@Override
					public synchronized void taskFailed(Future<Object> task,
							Throwable ex) {
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
						fSema.release();
						fReady.offer(result);
					}
				});
	}

	/**
	 * Starts a new block of tasks with a given size. This method should be
	 * called before any kind of submission is done.
	 * 
	 * @param task
	 *            the task title, to be used in an {@link IProgressTracker}, or
	 *            <code>null</code> if no progress tracking is desired.
	 * 
	 * @param taskBlock
	 *            the size of the task block; i.e., the number of calls to
	 *            {@link #submit(Callable)}/{@link #consume()} that this
	 *            {@link TaskExecutor} should expect to receive.
	 * 
	 */
	public synchronized void start(String task, int taskBlock) {
		fTasks = taskBlock;
		fConsumed = taskBlock;
		fBlockSize = taskBlock;
		fTracker = task == null ? Progress.nullTracker() : Progress
				.synchronizedTracker(Progress.newTracker(task, taskBlock));
		fTracker.startTask();
	}

	/**
	 * Convenience method. Same as: <BR>
	 * <code>
	 * 	start(null, taskBlock);
	 * </code>
	 * 
	 * @see #start(String, int)
	 */
	public void start(int taskBlock) {
		start(null, taskBlock);
	}

	public void submit(Callable<? extends Object> callable)
			throws InterruptedException {
		Semaphore sema;
		CallbackThreadPoolExecutor<Object> executor;
		synchronized (this) {
			if (fTasks == 0) {
				throw new IllegalStateException(
						"Task block is complete or haven't started.");
			}
			fTasks--;
			sema = fSema;
			executor = fExecutor;
		}

		sema.acquire();

		synchronized (this) {
			executor.submit(new TaskContainer(callable));
		}
	}

	public Object consume() throws InterruptedException {
		LinkedBlockingQueue<Object> queue;
		synchronized (this) {
			if (fConsumed == 0) {
				throw new IllegalStateException("All tasks have been consumed.");
			}
			fConsumed--;
			queue = fReady;
		}

		return queue.poll(Long.MAX_VALUE, TimeUnit.DAYS);
	}

	public synchronized void cancelBatch() {
		cancelBatchWithException(new InterruptedException(
				"Task batch interrupted."));
	}

	public synchronized void cancelBatchWithException(Exception error) {

		// Blocks new tasks from being submitted and consumed.
		fTasks = 0;
		fConsumed = 0;

		// Now we have to deal with the fact that:
		//
		// 1. there is an unknown number of consumers blocked on #consume();
		// 2. there are an unknown number of producers blocked on #submit().

		// To deal with consumers, we stuff the queue with exceptions. This way
		// we guarantee they will all unblock, and realize that there are no new
		// completed tasks from this batch coming their way.
		fillQueue(error);

		// To deal with producers, we stuff the semaphore with permits. This
		// way we guarantee they will not be kept on hold. This coupled with the
		// fact that we shutdown the executor without releasing the monitor lock
		// ensures that producers that are trying to submit will get a
		// RejectedExcecutionException, and realize someone interrupted the
		// current batch.
		fSema.release(fBlockSize);

		try {
			// Shuts down the executor.
			shutdown(true);

			// When shutdown returns, no more tasks can possibly be pending,
			// because we've shut down the executor synchronously.
			//
			// Now we replace the queue and the semaphore. New requests will use
			// these, while old ones, which might still be stuck inside of
			// submit/consume (waking up from the semaphore, or the poll call),
			// will have the old objects in their stacks.
			semaphoreAndQueue();

		} catch (InterruptedException ex) {
			throw new RuntimeException(
					"Shutdown interrupted, state might be inconsistent.");
		} finally {
			// Recreates the executor.
			createExecutor(fCores);
		}

	}

	/**
	 * @return an approximate number of the active tasks in the system.
	 */
	public int activeTasks() {
		return fActiveTasks.get();
	}

	private void signalActive() {
		if (fActiveTasks.incrementAndGet() > fCores) {
			fLogger.error("Active tasks exceeded available cores. This is a bug.");
		}
	}

	private void signalDone() {
		if (fActiveTasks.decrementAndGet() < 0) {
			fLogger.error("Activation signal lost.");
		}
	}

	private void semaphoreAndQueue() {
		fReady = new LinkedBlockingQueue<Object>();
		fSema = new Semaphore(fMaxQueuedTasks);
	}

	private void fillQueue(Exception ex) {
		for (int i = 0; i < fBlockSize; i++) {
			fReady.add(ex);
		}
	}

	public void shutdown(boolean wait) throws InterruptedException {
		fExecutor.shutdown();
		if (wait) {
			fExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		}
	}

	/**
	 * Instrumentation class which calls two hooks from inside of the executor's
	 * worker thread assigned to this task, which in turn allows us to track how
	 * many cores are active at a time for debugging and performance tuning
	 * purposes.
	 * 
	 * @author giuliano
	 */
	private class TaskContainer implements Callable<Object> {

		Callable<? extends Object> fTask;

		public TaskContainer(Callable<? extends Object> task) {
			fTask = task;
		}

		@Override
		public Object call() throws Exception {
			signalActive();
			try {
				return fTask.call();
			} finally {
				signalDone();
			}
		}

	}

}
