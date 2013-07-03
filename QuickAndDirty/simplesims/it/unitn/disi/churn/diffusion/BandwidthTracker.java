package it.unitn.disi.churn.diffusion;

import gnu.trove.list.array.TDoubleArrayList;

/**
 * {@link BandwidthTracker} bins continuous time into intervals of fixed length,
 * and tracks how many messages are received per interval.
 * 
 * @author giuliano
 */
public abstract class BandwidthTracker<T> implements Cloneable {

	private final TDoubleArrayList fDebug;

	private final double fBinWidth;

	private double fBinStart;

	private double fBase;

	private int fObservations;

	private int fCount;

	private int fTotalCount;

	// -------------------------------------------------------------------------

	public BandwidthTracker(double binWidth) {
		this(-1, binWidth, false);
	}

	// -------------------------------------------------------------------------

	public BandwidthTracker(double base, double binWidth) {
		this(base, binWidth, false);
	}

	// -------------------------------------------------------------------------

	public BandwidthTracker(double base, double binWidth, boolean debug) {
		if (binWidth <= 0) {
			throw new IllegalArgumentException("Bin width must be positive.");
		}

		fDebug = debug ? new TDoubleArrayList() : null;
		fBinWidth = binWidth;
		fBase = fBinStart = base < 0 ? Double.NEGATIVE_INFINITY : binBoundary(
				base, binWidth);
	}

	// -------------------------------------------------------------------------

	/**
	 * Counts a message in the current time bin.
	 */
	public void messageReceived() {
		fCount++;
	}

	// -------------------------------------------------------------------------

	/**
	 * Moves time to the provided instant. This will cause bin statistics to be
	 * collected and, optionally, all bins between the current one and the one
	 * delimited by the new time instant to be registered as zeros.
	 * 
	 * @param rawTime
	 *            the new time instant.
	 * 
	 * @param countGaps
	 *            whether or not to count bins in between as zeros.
	 */
	public BandwidthTracker<T> at(double rawTime, boolean countGaps) {
		if (rawTime < fBinStart) {
			throw new IllegalStateException("Time can't flow backwards.");
		}

		if (!withinBinBoundaries(rawTime)) {
			newBin(rawTime, countGaps);
		}
		
		return this;
	}

	// -------------------------------------------------------------------------

	/**
	 * Signals the end of the timeline.
	 * 
	 * @param time
	 *            the instant at which the timeline ends.
	 *            
	 * @param countGaps
	 *            whether to count the bins between the last one and the end of
	 *            the timeline as zeros.
	 */
	public void end(double time, boolean countGaps) {
		collectBinStatistics(time, true, countGaps);
		check(time);
		fBinStart = Double.NEGATIVE_INFINITY;
	}

	// -------------------------------------------------------------------------

	/**
	 * @return the current bin, or {@link Double#NEGATIVE_INFINITY} if the
	 *         current bin has not been defined yet.
	 */
	public double currentBin() {
		return fBinStart;
	}

	// -------------------------------------------------------------------------

	/**
	 * @return the cumulative number of messages received by this
	 *         {@link BandwidthTracker}.
	 */
	public int messageCount() {
		return fTotalCount;
	}

	// -------------------------------------------------------------------------

	/**
	 * @return the bin width.
	 */
	public double binWidth() {
		return fBinWidth;
	}

	// -------------------------------------------------------------------------

	/**
	 * @return the number of bins tracked by this {@link BandwidthTracker}, not
	 *         counting bins that have been skipped.
	 */
	public int observations() {
		return fObservations;
	}

	// -------------------------------------------------------------------------

	private void check(double time) {
		int expected = checkedCast(Math.ceil((time - fBase) / fBinWidth));
		if (expected != fObservations) {
			dumpDebugData(time);
			throw new IllegalStateException("Internal check failed.");
		}
	}
	
	// -------------------------------------------------------------------------

	private void dumpDebugData(double time) {
		if (fDebug != null) {
			for (int i = 0; i < fDebug.size(); i++) {
				System.out.println("OFF:" + fDebug.get(i));
			}
			System.out.println("OFF:" + time);
		}
	}
	
	// -------------------------------------------------------------------------

	private void newBin(double rawTime, boolean countGaps) {
		if (!firstBin()) {
			collectBinStatistics(rawTime, false, countGaps);
		} else {
			fBase = rawTime;
		}

		fBinStart = binBoundary(rawTime, fBinWidth);
		fCount = 0;
	}

	// -------------------------------------------------------------------------

	private boolean firstBin() {
		return fBinStart == Double.NEGATIVE_INFINITY;
	}
	
	// -------------------------------------------------------------------------

	private void collectBinStatistics(double rawTime, boolean includeCurrent,
			boolean includeZeros) {

		if (fDebug != null) {
			fDebug.add(rawTime);
		}

		addCount(fCount, 1);

		if (!includeZeros) {
			return;
		}

		int k = checkedCast(Math.floor((rawTime - fBinStart) / fBinWidth));
		// Truncation includes the current bin in the calculation as well.
		if (!includeCurrent) {
			k--;
		}

		if (k > 0) {
			addCount(0, k);
		}
	}

	private void addCount(int value, int n) {
		fObservations += n;
		fTotalCount += value;
		add(value, n);
	}

	private int checkedCast(double round) {
		if (round > Integer.MAX_VALUE) {
			throw new IllegalStateException("Cast failed.");
		}
		return (int) round;
	}

	private boolean withinBinBoundaries(double rawTime) {
		return rawTime < (fBinStart + fBinWidth);
	}

	private double binBoundary(double time, double width) {
		return Math.floor(time / width) * width;
	}

	public abstract T getStats();

	protected abstract void add(int value, int count);

}
