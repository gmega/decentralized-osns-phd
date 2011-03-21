package it.unitn.disi.random;

import java.util.Random;

/**
 * Generates a stream of exponentially distributed numbers.
 * 
 * @author giuliano
 */
public class Exponential implements IDistribution {

	private final double fLambda;
	
	private final Random fRand;
	
	public Exponential(double lambda, Random r) {
		fLambda = lambda;
		fRand = r;
	}
	
	@Override
	public double sample() {
		return -((1.0/fLambda) * Math.log(fRand.nextDouble()));
	}

	@Override
	public double expectation() {
		return 1.0/fLambda;
	}
}
