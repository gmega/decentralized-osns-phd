package it.unitn.disi.simulator.random;

import peersim.config.Attribute;
import peersim.config.AutoConfig;

/**
 * Generates a stream of exponentially distributed numbers.
 * 
 * Reference in <a href=
 * "http://en.wikipedia.org/wiki/Exponential_distribution#Generating_exponential_variates"
 * >Wikipedia</a>.
 * 
 * @author giuliano
 */
@AutoConfig
public class Exponential implements IDistribution {

	private final double fLambda;

	private final IDistribution fU;

	public Exponential(@Attribute("lambda") double lambda,
			@Attribute("UniformDistribution") IDistribution uniform) {
		fLambda = lambda;
		fU = uniform;
	}

	@Override
	public double sample() {
		return -((1.0 / fLambda) * Math.log(fU.sample()));
	}

	@Override
	public double expectation() {
		return 1.0 / fLambda;
	}
}
