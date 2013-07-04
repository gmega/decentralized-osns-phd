package it.unitn.disi.churn.diffusion;

import peersim.util.IncrementalStats;

public class BandwidthTrackerStats extends BandwidthTracker<IncrementalStats>
		implements Cloneable {

	private IncrementalStats fStats;

	public BandwidthTrackerStats(double binWidth) {
		this(-1, binWidth, false);
	}

	public BandwidthTrackerStats(double base, double binWidth) {
		this(base, binWidth, false);
	}

	public BandwidthTrackerStats(double base, double binWidth, boolean debug) {
		super(base, binWidth, debug);
		fStats = new IncrementalStats();
	}

	@Override
	protected void add(int value, int count) {
		fStats.add(value, count);
	}

	@Override
	public IncrementalStats getStats() {
		return fStats;
	}

	@Override
	public BandwidthTrackerStats clone() throws CloneNotSupportedException {
		BandwidthTrackerStats clone = (BandwidthTrackerStats) super.clone();
		clone.fStats = new IncrementalStats();
		clone.fStats.add(fStats);

		return clone;
	}
}
