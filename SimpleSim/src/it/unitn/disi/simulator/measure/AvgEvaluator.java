package it.unitn.disi.simulator.measure;

import java.io.Serializable;

import peersim.util.IncrementalStats;
import it.unitn.disi.statistics.StatUtils;

/**
 * {@link AvgEvaluator} applies standard statistical techniques to evaluate the
 * precision of a sample average. It can answer whether the number of collected
 * samples in an {@link IncrementalStats} object satisfies some target
 * confidence interval narrowness at 95% confidence.
 * 
 * @author giuliano
 */
public class AvgEvaluator implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final int MIN_SAMPLES = 100;

	public static final double DEFAULT_PRECISION = 0.05;

	private int fMinSamples;

	private double fResolutionLimit;

	private double fPrecision;

	public AvgEvaluator() {
		this(MIN_SAMPLES, DEFAULT_PRECISION, 0.0);
	}

	public AvgEvaluator(double precision, double reslimit) {
		this(MIN_SAMPLES, precision, reslimit);
	}

	/**
	 * Constructs an {@link AvgEvaluator}.
	 * 
	 * @param minSamples
	 *            the minimum number of samples required to boostrap the
	 *            evaluator. {@link #MIN_SAMPLES} is the recommended default.
	 * 
	 * @param precision
	 *            how far the upper confidence interval is allowed to be from
	 *            sample average before we declare the estimator to be precise
	 *            enough.
	 * 
	 * @param reslimit
	 *            absolute value under which the confidence interval is deemed
	 *            to be precise enough, regardless of how far from the sample
	 *            average (in percentage terms) it is. This is useful for very
	 *            small values.
	 * 
	 */
	public AvgEvaluator(int minSamples, double precision, double reslimit) {
		fPrecision = precision;
		fResolutionLimit = reslimit;
		fMinSamples = minSamples;
	}

	public double lowerConfidenceLimit(IncrementalStats stats) {
		if (!hasEnoughSamples(stats)) {
			return Double.NaN;
		}
		return StatUtils.lowerConfidenceLimit(stats);
	}

	public Double upperConfidenceLimit(IncrementalStats stats) {
		if (!hasEnoughSamples(stats)) {
			return Double.NaN;
		}
		return StatUtils.upperConfidenceLimit(stats);
	}

	public boolean isPrecise(IncrementalStats stats) {
		if (!hasEnoughSamples(stats)) {
			return false;
		}

		double upper = StatUtils.upperConfidenceLimit(stats);
		double avg = stats.getAverage();

		return isAbsolutePrecise(upper, avg) || isRelativePrecise(upper, avg);
	}

	private boolean hasEnoughSamples(IncrementalStats stats) {
		return stats.getN() >= fMinSamples;
	}

	private boolean isRelativePrecise(double upper, double avg) {
		return ((upper - avg) / Math.abs(avg)) < fPrecision;
	}

	private boolean isAbsolutePrecise(double upper, double avg) {
		return (upper - avg) < fResolutionLimit;
	}

}
