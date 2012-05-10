package it.unitn.disi.churn.simulator;

import it.unitn.disi.utils.CallbackThreadPoolExecutor;
import it.unitn.disi.utils.IExecutorCallback;
import it.unitn.disi.utils.logging.Progress;
import it.unitn.disi.utils.logging.ProgressTracker;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Helper class for running blocks of tasks and getting the results for
 * completed tasks first.
 * 
 * @author giuliano
 */
public class TaskExecutor {

	private final CallbackThreadPoolExecutor<Object> fExecutor;

	private final LinkedBlockingQueue<Object> fReady = new LinkedBlockingQueue<Object>(
			100);

	private volatile ProgressTracker fTracker;

	private int fTasks;

	private int fConsumed;

	public TaskExecutor(int cores) {

		fExecutor = new CallbackThreadPoolExecutor<Object>(cores > 0 ? cores
				: Runtime.getRuntime().availableProcessors(),
				new IExecutorCallback<Object>() {
					@Override
					public void taskFailed(Future<Object> task, Throwable ex) {
						fTracker.tick();
						queue(ex);
					}

					@Override
					public synchronized void taskDone(Object result) {
						fTracker.tick();
						queue(result);
					}

					private void queue(Object result) {
						try {
							fReady.offer(result, Long.MAX_VALUE, TimeUnit.DAYS);
						} catch (InterruptedException ex) {
							ex.printStackTrace();
						}
					}
				});

	}

	public void start(String task, int taskBlock) {
		fTasks = fConsumed = taskBlock;
		fTracker = Progress.newTracker(task, taskBlock);
		fTracker.startTask();
	}

	public void submit(Callable<? extends Object> callable) {
		if (fTasks == 0) {
			throw new IllegalStateException("Task block is complete.");
		}
		fExecutor.submit(callable);
		fTasks--;
	}

	public Object consume() throws InterruptedException {
		if (fConsumed == 0) {
			throw new IllegalStateException("All tasks have been consumed.");
		}
		fConsumed--;
		return fReady.poll(Long.MAX_VALUE, TimeUnit.DAYS);
	}

	public void shutdown(boolean wait) throws InterruptedException {
		fExecutor.shutdown();
		if (wait) {
			fExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		}
	}

}
