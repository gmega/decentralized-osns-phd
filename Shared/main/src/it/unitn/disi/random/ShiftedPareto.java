package it.unitn.disi.random;

/**
 * Generates a stream of numbers distributed according to a "shifted Pareto"
 * distribution, that is, a Pareto distribution whose PDF has been shifted to
 * start at x = 0.
 * 
 * @author giuliano
 * 
 */
public class ShiftedPareto implements IDistribution {

	private final double fInverseAlpha;

	private final double fBeta;

	public ShiftedPareto(double alpha, double beta) {
		fBeta = beta;
		fInverseAlpha = 1.0 / alpha;
	}

	@Override
	public double sample() {
		return fBeta * (Math.pow((1.0 - Math.random()), -fInverseAlpha) - 1);
	}

}
