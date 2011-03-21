package it.unitn.disi.random;

import java.util.Random;

/**
 * Generates a stream of Weibull distributed numbers.
 * 
 * @author giuliano
 */
public class Weibull implements IDistribution {

	private final double fShapeInverse;

	private final double fScale;
	
	private final Random fRand;

	/**
	 * Creates a new Weibull-distributed random number generator.
	 * 
	 * @param shape
	 *            the shape of the distribution.
	 * @param scale
	 *            the scale of the distribution.
	 */
	public Weibull(double shape, double scale, Random r) {
		fShapeInverse = 1.0 / shape;
		fScale = scale;
		fRand = r;
	}

	@Override
	public double sample() {
		return fScale * Math.pow(-Math.log(1.0 - fRand.nextDouble()), fShapeInverse);
	}

	@Override
	public double expectation() {
		throw new UnsupportedOperationException("BUG: not implemented (required gamma function).");
	}

}
