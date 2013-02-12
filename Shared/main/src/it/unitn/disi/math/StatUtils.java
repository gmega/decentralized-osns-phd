package it.unitn.disi.math;

import peersim.util.IncrementalStats;

public class StatUtils {

	private static final double N_QUANTILE = 1.959964;

	public static double upperConfidenceLimit(IncrementalStats stats) {
		return stats.getAverage() + N_QUANTILE
				* Math.sqrt(stats.getVar() / stats.getN());
	}

	public static double lowerConfidenceLimit(IncrementalStats stats) {
		return stats.getAverage() - N_QUANTILE
				* Math.sqrt(stats.getVar() / stats.getN());
	}

}
