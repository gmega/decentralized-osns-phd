package it.unitn.disi.utils.logging;

import org.apache.log4j.Logger;

public class Progress {

	public static IProgressTracker newTracker(String title, int totalTicks) {
		return new TextProgressTracker(title, totalTicks) {
			@Override
			protected void out(String out) {
				System.err.println(out);
			}
		};
	}

	public static IProgressTracker newTracker(String title, int totalTicks,
			final Logger output) {
		return new TextProgressTracker(title, totalTicks) {
			@Override
			protected void out(String out) {
				output.info(out);
			}
		};
	}

	public static IProgressTracker synchronizedTracker(
			final IProgressTracker delegate) {
		return new IProgressTracker() {

			@Override
			public synchronized void startTask() {
				delegate.startTask();
			}

			@Override
			public synchronized void tick() {
				delegate.tick();
			}

			@Override
			public synchronized void tick(int ticks) {
				delegate.tick(ticks);
			}

			@Override
			public synchronized void done() {
				delegate.done();
			}

			@Override
			public synchronized String title() {
				return delegate.title();
			}
			
		};
	}
	
	public static IProgressTracker nullTracker() {
		return IProgressTracker.NULL_TRACKER;
	}
}
