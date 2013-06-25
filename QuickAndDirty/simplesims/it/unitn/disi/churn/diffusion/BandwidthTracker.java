package it.unitn.disi.churn.diffusion;

import peersim.util.IncrementalStats;

/**
 * {@link BandwidthTracker} bins continuous time into intervals of fixed length,
 * and tracks how many messages are received per interval.
 * 
 * @author giuliano
 */
public class BandwidthTracker {

	private final IncrementalStats fStats;

	private final double fBinWidth;

	private double fBinStart;

	private int fCount;

	public BandwidthTracker(double binWidth) {
		fStats = new IncrementalStats();
		fBinStart = Double.NEGATIVE_INFINITY;
		fBinWidth = binWidth;
	}

	public IncrementalStats getStats() {
		return fStats;
	}

	public void messageReceived(double rawTime) {
		if (rawTime < fBinStart) {
			throw new IllegalStateException("Time can't flow backwards.");
		}

		if (!withinBinBoundaries(rawTime)) {
			newBin(rawTime);
		}

		fCount++;
	}

	public void truncate() {
		fStats.add(fCount);
		fBinStart = Double.NEGATIVE_INFINITY;
	}

	private void newBin(double rawTime) {
		if (!firstBin()) {
			collectBinStatistics(rawTime);
		}

		fBinStart = binBoundary(rawTime);
		fCount = 0;
	}

	private boolean firstBin() {
		return fBinStart == Double.NEGATIVE_INFINITY;
	}

	private void collectBinStatistics(double rawTime) {
		fStats.add(fCount);
		int k = checkedCast(Math.round((rawTime - fBinStart) / fBinWidth)) - 1;
		if (k > 0) {
			fStats.add(0, k);
		}
	}

	private int checkedCast(long round) {
		if (round > Integer.MAX_VALUE) {
			throw new IllegalStateException("Cast failed.");
		}
		return (int) round;
	}

	private boolean withinBinBoundaries(double rawTime) {
		return rawTime < (fBinStart + fBinWidth);
	}

	private double binBoundary(double rawTime) {
		return Math.floor(rawTime / fBinWidth) * fBinWidth;
	}

}
