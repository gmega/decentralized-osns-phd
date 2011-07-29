package it.unitn.disi.utils.logging;

import org.apache.log4j.Logger;

public class Progress {
	public static ProgressTracker newTracker(String title, int totalTicks) {
		return new TextProgressTracker(title, totalTicks) {
			@Override
			protected void out(String out) {
				System.err.println(out);
			}
		};
	}
	
	public static ProgressTracker newTracker(String title, int totalTicks,
			final Logger output) {
		return new TextProgressTracker(title, totalTicks) {
			@Override
			protected void out(String out) {
				output.info(out);
			}
		};
	}
}
