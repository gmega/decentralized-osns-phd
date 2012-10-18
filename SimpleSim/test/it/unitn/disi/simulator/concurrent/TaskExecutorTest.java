package it.unitn.disi.simulator.concurrent;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.RejectedExecutionException;

import junit.framework.Assert;

import org.junit.Test;

public class TaskExecutorTest {

	volatile TaskExecutor fExecutor;

	@Test
	public void testSubmission() throws Exception {
		fExecutor = new TaskExecutor(4, 4);
		fExecutor.start("test task", 30);
		ArrayList<Submitter> submitters = new ArrayList<Submitter>();
		ArrayList<Consumer> consumers = new ArrayList<Consumer>();
		for (int i = 0; i < 30; i++) {
			Submitter sub = new Submitter();
			submitters.add(sub);

			Consumer cons = new Consumer();
			consumers.add(cons);

			new Thread(sub, "Submitter " + i).start();
			new Thread(cons, "Consumer " + i).start();
		}

		Thread.sleep(1000);

		fExecutor.cancelBatch();
		int rejected = 0;
		int none = 0;
		
		Thread.sleep(1000);

		for (Submitter submitter : submitters) {
			if (submitter.exception() == null) {
				none++;
			}

			if (submitter.exception() instanceof RejectedExecutionException) {
				rejected++;
			}
		}

		Assert.assertEquals(4, none);
		Assert.assertEquals(26, rejected);
		
		for (Consumer cons : consumers) {
			Assert.assertTrue(cons.exception() instanceof InterruptedException);
		}

	}

	class Submitter implements Runnable {

		private Exception fException;

		@Override
		public void run() {
			try {
				fExecutor.submit(new Task());
				System.err.println("Submit ok!");
			} catch (Exception e) {
				e.printStackTrace();
				fException = e;
			}
		}

		public Exception exception() {
			return fException;
		}

	}

	class Consumer implements Runnable {

		private Exception fException;

		@Override
		public void run() {
			try {
				Object value = fExecutor.consume();
				if (value instanceof Exception) {
					fException = (Exception) value;
				}
			} catch (InterruptedException e) {

			}
		}

		public Exception exception() {
			return fException;
		}

	}

	class Task implements Callable<Object> {

		@Override
		public Object call() {
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return new Object();
		}

	}
}
