package it.unitn.disi.simulator.measure;

import peersim.util.IncrementalStats;

public class AvgAccumulation implements IMetricAccumulator<Double> {

	private static final long serialVersionUID = 1L;

	private IncrementalStats[] fMetric;

	private Object fId;

	private boolean fPrecise;
	
	private AvgEvaluator fTester;

	public AvgAccumulation(Object id, int length) {
		this(id, length, AvgEvaluator.DEFAULT_PRECISION, 0.0);
	}

	public AvgAccumulation(Object id, int length, double precision,
			double reslimit) {
		fId = id;
		fMetric = new IncrementalStats[length];
		for (int i = 0; i < fMetric.length; i++) {
			fMetric[i] = new IncrementalStats();
		}
		fTester = new AvgEvaluator(precision, reslimit);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.unitn.disi.simulator.measure.IAccumulatingMetric#id()
	 */
	@Override
	public Object id() {
		return fId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.unitn.disi.simulator.measure.IAccumulatingMetric#getMetric(int)
	 */
	@Override
	public Double getMetric(int i) {
		return fMetric[i].getAverage();
	}
	
	public Double lowerConfidenceLimit(int i) {
		return fTester.lowerConfidenceLimit(fMetric[i]);
	}
	
	public Double upperConfidenceLimit(int i) {
		return fTester.upperConfidenceLimit(fMetric[i]);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * it.unitn.disi.simulator.measure.IAccumulatingMetric#add(it.unitn.disi
	 * .simulator.measure.INetworkMetric)
	 */
	@Override
	public void add(INodeMetric<Double> metric) {
		fPrecise = true;
		for (int i = 0; i < fMetric.length; i++) {
			fMetric[i].add(metric.getMetric(i));
			fPrecise &= fTester.isPrecise(fMetric[i]);
		}
	}

	@Override
	public boolean isPreciseEnough() {
		return fPrecise;
	}

}
