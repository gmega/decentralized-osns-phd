package it.unitn.disi.churn.diffusion;

import peersim.extras.am.util.IncrementalStatsFreq;

public class BandwidthTrackerFreq extends
		BandwidthTracker<IncrementalStatsFreq> {

	private IncrementalStatsFreq fStats;

	public BandwidthTrackerFreq(double binWidth) {
		this(-1, binWidth);
	}

	public BandwidthTrackerFreq(double base, double binWidth) {
		this(base, 0.0, binWidth);
	}
	
	public BandwidthTrackerFreq(double base, double uptimeBase, double binWidth) {
		super(base, uptimeBase, binWidth);
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

	@Override
	public BandwidthTrackerFreq clone() throws CloneNotSupportedException {
		BandwidthTrackerFreq clone = (BandwidthTrackerFreq) super.clone();
		clone.fStats = (IncrementalStatsFreq) fStats.clone();
		return clone;
	}

}
