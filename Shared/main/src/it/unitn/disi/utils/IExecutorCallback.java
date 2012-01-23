package it.unitn.disi.utils;

import java.util.concurrent.Future;

public interface IExecutorCallback<T> {
	public void taskFailed(Future<T> task, Exception ex);
	
	public void taskDone(T result);
}
