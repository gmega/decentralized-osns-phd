package it.unitn.disi.simulator.random;

import peersim.config.Attribute;
import peersim.config.AutoConfig;

/**
 * Generates a stream of Weibull distributed numbers.
 * 
 * @author giuliano
 */
@AutoConfig
public class Weibull implements IDistribution {

	private final double fShapeInverse;

	private final double fScale;
	
	private final IDistribution fU;

	/**
	 * Creates a new Weibull-distributed random number generator.
	 * 
	 * @param shape
	 *            the shape of the distribution.
	 * @param scale
	 *            the scale of the distribution.
	 */
	public Weibull(@Attribute("shape") double shape, 
			@Attribute("scale") double scale, 
			@Attribute("UniformDistribution") IDistribution uniform) {
		fShapeInverse = 1.0 / shape;
		fScale = scale;
		fU = uniform;
	}

	@Override
	public double sample() {
		return fScale * Math.pow(-Math.log(1.0 - fU.sample()), fShapeInverse);
	}

	@Override
	public double expectation() {
		throw new UnsupportedOperationException("BUG: not implemented (requires the gamma function).");
	}

}
