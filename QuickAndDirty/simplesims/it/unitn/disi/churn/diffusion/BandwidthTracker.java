package it.unitn.disi.churn.diffusion;

import it.unitn.disi.simulator.core.IClockData;
import it.unitn.disi.simulator.core.IProcess;

/**
 * {@link BandwidthTracker} bins continuous time into intervals of fixed length,
 * and tracks how many messages are received per interval.
 * 
 * @author giuliano
 */
public abstract class BandwidthTracker<T> implements Cloneable {

	private final double fBinWidth;

	private double fRawTime;

	private double fBase;

	private double fUptimeBase;

	private int fObservations;

	private int fCount;

	private int fTotalCount;

	// -------------------------------------------------------------------------

	public BandwidthTracker(double binWidth) {
		this(-1, binWidth);
	}
	
	// -------------------------------------------------------------------------
	
	public BandwidthTracker(double base, double binWidth) {
		this(base, 0.0, binWidth);
	}

	// -------------------------------------------------------------------------

	public BandwidthTracker(double base, double uptimeBase, double binWidth) {
		if (binWidth <= 0 || uptimeBase < 0) {
			throw new IllegalArgumentException("Bin width must be positive"
					+ ", and uptime base needs to be strictly positive.");
		}

		fBinWidth = binWidth;
		fBase = fRawTime = base < 0 ? Double.NEGATIVE_INFINITY : base;
		fUptimeBase = uptimeBase;
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
	 * Moves time to the provided instant, possibly changing the current time
	 * bin.
	 * 
	 * @param rawTime
	 *            the new time instant.
	 * 
	 */
	public BandwidthTracker<T> at(double rawTime) {
		ensureForwardFlow(rawTime);

		if (rawTime >= nextBinBoundary(fRawTime, fBinWidth)) {
			newBin(rawTime);
		}

		return this;
	}

	// -------------------------------------------------------------------------

	/**
	 * Signals the end of the timeline at the time instant registered by the
	 * last call to {@link #at(double)}.<BR>
	 * <BR>
	 * <b>Handling of the last bin:</b> the last bin in the sequence always
	 * counts as a full bin. So, for example, if we have a bin width of 1
	 * second, and messages are received at instants 1, 2.5, and 3.5, and then
	 * {@link #end()} is called, we count the last bin, i.e. bin [3, 4), as
	 * having only one message.
	 * 
	 * More surprisingly, however, if messages are received at instants 1, 2,
	 * and 3, and then the client calls: <BR>
	 * 
	 * <code>
	 * 	at(4.0).end();
	 * </code>
	 * 
	 * we count the last bin, i.e. [4, 5) in this case, to be counted as having
	 * zero messages. This might introduce one zero more than expected.
	 * 
	 */
	public void end() {
		lastCount();
		addCount(0, zeroBucketCount(fRawTime - fBase, true));
		fRawTime = Double.NEGATIVE_INFINITY;
	}

	// -------------------------------------------------------------------------

	/**
	 * Signals the end of the timeline at the current uptime of a process.
	 * 
	 * @param process
	 * @param clock
	 */
	public void end(IProcess process, IClockData clock) {
		ensureForwardFlow(clock.rawTime());
		lastCount();
		addCount(0, zeroBucketCount(process.uptime(clock) - fUptimeBase, true));
		fRawTime = Double.NEGATIVE_INFINITY;
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

	@Override
	@SuppressWarnings("unchecked")
	public BandwidthTracker<T> clone() throws CloneNotSupportedException {
		return (BandwidthTracker<T>) super.clone();
	}

	// -------------------------------------------------------------------------

	private int zeroBucketCount(double time, boolean slack) {
		int count = checkedCast(Math.ceil(time / fBinWidth)) - fObservations;

		if (count < 0) {
			// slack is a hack until I figure out how to work this properly.
			if (slack) {
				System.out.println("SLCK:" + count);
			} else {
				throw new IllegalStateException(Integer.toString(count));	
			}
		}

		return Math.max(0, count);
	}

	// -------------------------------------------------------------------------

	private void lastCount() {
		if (fCount > 0) {
			addCount(fCount, 1);
		}
	}

	// -------------------------------------------------------------------------

	private void ensureForwardFlow(double rawTime) throws IllegalStateException {
		if (rawTime < fRawTime) {
			throw new IllegalStateException("Time can't flow backwards.");
		}
	}

	// -------------------------------------------------------------------------

	private void newBin(double rawTime) {
		if (!firstBin()) {
			addCount(fCount, 1);
		} else {
			fBase = rawTime;
		}
		fRawTime = rawTime;
		fCount = 0;
	}

	// -------------------------------------------------------------------------

	private boolean firstBin() {
		return fRawTime == Double.NEGATIVE_INFINITY;
	}

	// -------------------------------------------------------------------------

	private void addCount(int value, int n) {
		fObservations += n;
		fTotalCount += value;
		add(value, n);
	}

	// -------------------------------------------------------------------------

	private int checkedCast(double round) {
		if (round > Integer.MAX_VALUE) {
			throw new IllegalStateException("Cast failed.");
		}
		return (int) round;
	}

	// -------------------------------------------------------------------------

	private double nextBinBoundary(double time, double width) {
		return (Math.floor(time / width) + 1) * width;
	}

	// -------------------------------------------------------------------------

	public abstract T getStats();

	protected abstract void add(int value, int count);

}
