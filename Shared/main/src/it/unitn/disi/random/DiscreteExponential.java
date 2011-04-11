package it.unitn.disi.random;

import java.util.Random;

/**
 * Implements a simple discrete exponential distribution that can be
 * parameterized by a single probability value. Sampling from it can
 * be somewhat expensive.
 * 
 * @author giuliano
 */
public class DiscreteExponential implements IDistribution {

	private final double fP;

	private final Random fRandom;

	public DiscreteExponential(double p, Random random) {
		fRandom = random;
		fP = p;
	}

	@Override
	public double sample() {
		int trials = 1;
		while (fRandom.nextDouble() >= fP) {
			trials++;
		}
		return trials;
	}

	@Override
	public double expectation() {
		return 1.0/(fP);
	}

}
