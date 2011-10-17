package it.unitn.disi.random;

import java.util.Random;

/**
 * Generates a stream of numbers distributed according to a "shifted Pareto"
 * distribution, that is, a Pareto distribution whose PDF has been shifted to
 * start at x = 0.<BR>
 * <BR>
 * Formulas for sampling and expectation taken from the generalized Pareto
 * distribution on <a href= "http://en.wikipedia.org/wiki/Pareto_distribution"
 * >Wikipedia</a>, but with parameters properly converted from <a
 * href="http://dx.doi.org/10.1109/ICNP.2006.320196">Yao's heterogeneous churn
 * paper</a>.
 * 
 * @author giuliano
 * 
 */
public class ShiftedPareto implements IDistribution {

	private final double fAlpha;

	private final double fBeta;

	private final IDistribution fU;

	public ShiftedPareto(double alpha, double beta, IDistribution uniform) {
		fBeta = beta;
		fAlpha = alpha;
		fU = uniform;
	}

	@Override
	public double sample() {
		return fBeta * (Math.pow(fU.sample(), -1.0 / fAlpha) - 1);
	}

	@Override
	public double expectation() {
		return fBeta / (fAlpha - 1.0);
	}

}
