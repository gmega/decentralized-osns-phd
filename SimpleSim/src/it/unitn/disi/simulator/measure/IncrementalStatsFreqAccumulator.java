package it.unitn.disi.simulator.measure;

import peersim.extras.am.util.IncrementalStatsFreq;

public class IncrementalStatsFreqAccumulator implements
		IMetricAccumulator<IncrementalStatsFreq> {

	private static final long serialVersionUID = 1L;

	private final String fId;

	private final IncrementalStatsFreq[] fStats;

	public IncrementalStatsFreqAccumulator(String id, int size) {
		fStats = new IncrementalStatsFreq[size];
		for (int i = 0; i < size; i++) {
			fStats[i] = new IncrementalStatsFreq();
		}
		fId = id;
	}

	@Override
	public void add(INodeMetric<IncrementalStatsFreq> metric) {
		for (int i = 0; i < fStats.length; i++) {
			fStats[i].add(metric.getMetric(i));
		}
	}

	@Override
	public boolean isPreciseEnough() {
		return false;
	}

	@Override
	public Object id() {
		return fId;
	}

	@Override
	public IncrementalStatsFreq getMetric(int i) {
		return fStats[i];
	}

}
