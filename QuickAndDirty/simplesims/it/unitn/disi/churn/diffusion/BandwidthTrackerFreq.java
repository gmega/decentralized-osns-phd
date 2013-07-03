package it.unitn.disi.churn.diffusion;

import peersim.extras.am.util.IncrementalStatsFreq;

public class BandwidthTrackerFreq extends
		BandwidthTracker<IncrementalStatsFreq> {

	private final IncrementalStatsFreq fStats;

	public BandwidthTrackerFreq(double binWidth) {
		this(-1, binWidth, false);
	}

	public BandwidthTrackerFreq(double base, double binWidth) {
		this(base, binWidth, false);
	}

	public BandwidthTrackerFreq(double base, double binWidth, boolean debug) {
		super(base, binWidth, debug);
		fStats = new IncrementalStatsFreq();
	}

	@Override
	protected void add(int value, int count) {
		if (count == 1) {
			fStats.add(value);
			return;
		}

		for (int i = 0; i < count; i++) {
			fStats.add(value);
		}
	}

	@Override
	public IncrementalStatsFreq getStats() {
		return fStats;
	}

}
