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

import org.apache.log4j.Logger;

/**
 * Helper class for running blocks of tasks and getting the results for
 * completed tasks first. There are plenty of ways of using this class wrong.
 * 
 * @author giuliano
 */
public class TaskExecutor {

	private static final Logger fLogger = Logger.getLogger(TaskExecutor.class);

	private volatile CallbackThreadPoolExecutor<Object> fExecutor;

	private volatile LinkedBlockingQueue<Object> fReady = new LinkedBlockingQueue<Object>();

	private volatile Semaphore fSema;

	private final int fCores;

	private int fBlockSize;

	private int fTasks;

	private int fConsumed;

	private volatile IProgressTracker fTracker;

	private boolean fDiscard;

	private int fMaxQueuedTasks;

	public TaskExecutor(int cores) {
		this(cores, Integer.MAX_VALUE);
	}

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
						if (fDiscard) {
							return;
						}
						fSema.release();
						fReady.offer(result);
					}
				});
	}

	public synchronized void start(String task, int taskBlock) {
		fTasks = taskBlock;
		fConsumed = taskBlock;
		fBlockSize = taskBlock;
		fTracker = Progress.synchronizedTracker(Progress.newTracker(task,
				taskBlock));
		fTracker.startTask();
	}

	public void submit(Callable<? extends Object> callable)
			throws InterruptedException {
		Semaphore sema;
		CallbackThreadPoolExecutor<Object> executor;
		synchronized (this) {
			if (fTasks == 0) {
				throw new IllegalStateException("Task block is complete.");
			}
			fTasks--;
			sema = fSema;
			executor = fExecutor;
		}

		sema.acquire();

		synchronized (this) {
			executor.submit(callable);
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

		// Blocks new tasks from being submitted and consumed.
		fTasks = 0;
		fConsumed = 0;

		// Now we have to deal with the fact that:
		//
		// 1. there is an unknown number of consumers blocked on #consume();
		// 2. there are an unknown number of producers blocked on #submit().

		// To deal with consumers, we stuff the queue with exceptions. This way
		// we guarantee they will all unblock.
		fillQueue();

		// To deal with producers, we add all permits the semaphore needs. This
		// way we guarantee they will not be kept on hold.
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

	public void semaphoreAndQueue() {
		fReady = new LinkedBlockingQueue<Object>();
		fSema = new Semaphore(fMaxQueuedTasks);
	}

	private void fillQueue() {
		InterruptedException ex = new InterruptedException(
				"Task batch interrupted.");
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

}
