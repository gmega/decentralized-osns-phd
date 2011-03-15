package it.unitn.disi.random;

/**
 * Generates a stream of Weibull distributed numbers.
 * 
 * @author giuliano
 */
public class Weibull implements IDistribution {

	private final double fShapeInverse;

	private final double fScale;

	/**
	 * Creates a new Weibull-distributed random number generator.
	 * 
	 * @param shape
	 *            the shape of the distribution.
	 * @param scale
	 *            the scale of the distribution.
	 */
	public Weibull(double shape, double scale) {
		fShapeInverse = 1.0 / shape;
		fScale = scale;
	}

	@Override
	public double sample() {
		return fScale * Math.pow(-Math.log(1.0 - Math.random()), fShapeInverse);
	}

}
