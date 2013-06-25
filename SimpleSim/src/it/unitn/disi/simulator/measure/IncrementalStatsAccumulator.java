package it.unitn.disi.simulator.measure;

import peersim.util.IncrementalStats;

public class IncrementalStatsAccumulator implements
		IMetricAccumulator<IncrementalStats> {

	private static final long serialVersionUID = 1L;

	private final String fId;
	
	private final IncrementalStats[] fStats;

	public IncrementalStatsAccumulator(String id, int size) {
		fStats = new IncrementalStats[size];
		for (int i = 0; i < size; i++) {
			fStats[i] = new IncrementalStats();
		}
		fId = id;
	}

	@Override
	public void add(INodeMetric<IncrementalStats> metric) {
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
	public IncrementalStats getMetric(int i) {
		return fStats[i];
	}

}
