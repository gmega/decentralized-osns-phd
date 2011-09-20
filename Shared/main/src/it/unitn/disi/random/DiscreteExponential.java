package it.unitn.disi.random;

import java.util.Random;

/**
 * Implements a simple discrete exponential distribution that can be
 * parameterized by a single probability value. We use a form of rejection
 * sampling here, so it can be somewhat expensive.
 * 
 * @author giuliano
 */
public class DiscreteExponential implements IDistribution {
	
	public static DiscreteExponential withProbability(double p, Random r) {
		return new DiscreteExponential(p, r);
	}
	
	public static DiscreteExponential withAverage(double avg, Random r) {
		return withProbability((avg)/(avg + 1.0), r);
	}

	private final double fP;

	private final Random fRandom;
	
	private DiscreteExponential(double p, Random random) {
		fRandom = random;
		fP = p;
	}

	@Override
	public double sample() {
		int trials = 0;
		while (fRandom.nextDouble() < fP) {
			trials++;
		}
		return trials;
	}

	@Override
	public double expectation() {
		return 1.0 / (1.0 - fP) - 1.0;
	}

}
