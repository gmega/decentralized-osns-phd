package it.unitn.disi.random;

/**
 * Generates a stream of exponentially distributed numbers.
 * 
 * @author giuliano
 */
public class Exponential implements IDistribution {

	private final double fLambda;
	
	private final IDistribution fU;;
	
	public Exponential(double lambda, IDistribution uniform) {
		fLambda = lambda;
		fU = uniform;
	}
	
	@Override
	public double sample() {
		return -((1.0/fLambda) * Math.log(fU.sample()));
	}

	@Override
	public double expectation() {
		return 1.0/fLambda;
	}
}
