package it.unitn.disi.utils.logging;

public class Progress {
	public static ProgressTracker newTracker(String title, int totalTicks) {
		return new TextProgressTracker(title, totalTicks);
	}
}
