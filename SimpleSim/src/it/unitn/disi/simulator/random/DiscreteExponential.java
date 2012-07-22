package it.unitn.disi.simulator.random;

import peersim.config.AutoConfig;

/**
 * Implements a simple discrete exponential distribution that can be
 * parameterized by a single probability value. We use a form of rejection
 * sampling here, so it can be somewhat expensive.
 * 
 * @author giuliano
 */
@AutoConfig
public class DiscreteExponential implements IDistribution {
	
	public static DiscreteExponential withProbability(double p,
			IDistribution unif) {
		return new DiscreteExponential(p, unif);
	}

	public static DiscreteExponential withAverage(double avg, IDistribution unif) {
		return withProbability((avg) / (avg + 1.0), unif);
	}

	private final double fP;

	private final IDistribution fUnif;
	
	private DiscreteExponential(double p, IDistribution unif) {
		fUnif = unif;
		fP = p;
	}

	@Override
	public double sample() {
		int trials = 0;
		while (fUnif.sample() < fP) {
			trials++;
		}
		return trials;
	}

	@Override
	public double expectation() {
		return 1.0 / (1.0 - fP) - 1.0;
	}

}
