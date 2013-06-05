package it.unitn.disi.churn;

import it.unitn.disi.simulator.random.IDistribution;

public class PredefinedDistribution implements IDistribution {

	private double[] fValues;

	private int fIndex;

	public PredefinedDistribution(double[] values) {
		fValues = values;
	}

	@Override
	public double sample() {
		double next = fValues[fIndex];
		fIndex++;
		return next;
	}

	@Override
	public double expectation() {
		return Double.NaN;
	}

}
