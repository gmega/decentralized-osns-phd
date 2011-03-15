package it.unitn.disi.random;

/**
 * Generates a stream of exponentially distributed numbers.
 * 
 * @author giuliano
 */
public class Exponential implements IDistribution {

	private final double fLambda;
	
	public Exponential(double lambda) {
		fLambda = lambda;
	}
	
	@Override
	public double sample() {
		return -((1.0/fLambda) * Math.log(Math.random()));
	}

}
