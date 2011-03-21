package it.unitn.disi.random;

import java.util.Random;

/**
 * Generates a stream of numbers distributed according to a "shifted Pareto"
 * distribution, that is, a Pareto distribution whose PDF has been shifted to
 * start at x = 0.
 * 
 * @author giuliano
 * 
 */
public class ShiftedPareto implements IDistribution {

	private final double fAlpha;

	private final double fBeta;
	
	private final Random fRand;

	public ShiftedPareto(double alpha, double beta, Random r) {
		fBeta = beta;
		fAlpha = alpha;
		fRand = r;
	}

	@Override
	public double sample() {
		// From Wikipedia. 
		double sample = fBeta*(Math.pow(fRand.nextDouble(), -1.0/fAlpha) - 1);
		if (sample < 0) {
			throw new InternalError();
		}
		return sample;
	}

	@Override
	public double expectation() {
		return fBeta/(fAlpha - 1.0);
	}

}
