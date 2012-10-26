package it.unitn.disi.simulator.measure;

import it.unitn.disi.statistics.StatUtils;
import peersim.util.IncrementalStats;

public class AvgAccumulation implements IMetricAccumulator<Double> {

	private static final long serialVersionUID = 1L;

	private static final int LOWER_SIGNIFICANCE_BOUND = 100;

	public static final double DEFAULT_PRECISION = 0.05;

	private IncrementalStats[] fMetric;

	private Object fId;

	private double fResolutionLimit;

	private double fPrecision;

	private boolean fPrecise;

	public AvgAccumulation(Object id, int length) {
		this(id, length, DEFAULT_PRECISION, 0.0);
	}

	public AvgAccumulation(Object id, int length, double precision,
			double reslimit) {
		fId = id;
		fMetric = new IncrementalStats[length];
		for (int i = 0; i < fMetric.length; i++) {
			fMetric[i] = new IncrementalStats();
		}
		fPrecision = precision;
		fResolutionLimit = reslimit;
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
		if(!hasEnoughSamples(fMetric[i])) {
			return Double.NaN;
		}
		return StatUtils.lowerConfidenceLimit(fMetric[i]);
	}
	
	public Double upperConfidenceLimit(int i) {
		if(!hasEnoughSamples(fMetric[i])) {
			return Double.NaN;
		}
		return StatUtils.upperConfidenceLimit(fMetric[i]);	
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
			fPrecise &= isPrecise(fMetric[i]);
		}
	}

	private boolean isPrecise(IncrementalStats stats) {
		if(!hasEnoughSamples(stats)) {
			return false;
		}
		
		double upper = StatUtils.upperConfidenceLimit(stats);
		double avg = stats.getAverage();

		return isAbsolutePrecise(upper, avg) || isRelativePrecise(upper, avg);
	}

	public boolean hasEnoughSamples(IncrementalStats stats) {
		return stats.getN() >= LOWER_SIGNIFICANCE_BOUND;
	}

	private boolean isRelativePrecise(double upper, double avg) {
		return ((upper - avg) / Math.abs(avg)) < fPrecision;
	}

	private boolean isAbsolutePrecise(double upper, double avg) {
		return (upper - avg) < fResolutionLimit;
	}

	@Override
	public boolean isPreciseEnough() {
		return fPrecise;
	}

}
