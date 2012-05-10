package it.unitn.disi.utils;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class CallbackThreadPoolExecutor<T> extends ThreadPoolExecutor {

	private final IExecutorCallback<T> fCallback;

	public CallbackThreadPoolExecutor(int cores, IExecutorCallback<T> loadSim) {
		super(cores, cores, 0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>());
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
			System.err.println("Error while retrieving task results.");
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
