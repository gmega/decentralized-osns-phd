package it.unitn.disi.utils;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class CallbackThreadPoolExecutor<T> extends ThreadPoolExecutor {

	private final IExecutorCallback<T> fCallback;

	public CallbackThreadPoolExecutor(int cores, IExecutorCallback<T> loadSim) {
		this(cores, loadSim, new LinkedBlockingQueue<Runnable>());
	}

	public CallbackThreadPoolExecutor(int cores, IExecutorCallback<T> loadSim,
			BlockingQueue<Runnable> queue) {
		super(cores, cores, 0L, TimeUnit.MILLISECONDS, queue);
		fCallback = loadSim;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void afterExecute(Runnable r, Throwable t) {
		Future<T> future = (Future<T>) r;

		T results = null;

		try {
			results = future.get();
		} catch (Throwable ex) {
			if (ex instanceof ExecutionException) {
				ex = (Throwable) ex.getCause();
			}
			fCallback.taskFailed(future, ex);
			if (ex instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			this.shutdown();
			return;
		}

		fCallback.taskDone(results);
	}
}
