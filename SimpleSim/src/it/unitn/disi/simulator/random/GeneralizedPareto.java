package it.unitn.disi.simulator.random;

import peersim.config.Attribute;
import peersim.config.AutoConfig;

/**
 * Random number generator for a generalized Pareto distribution.<BR>
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
@AutoConfig
public class GeneralizedPareto implements IDistribution {

	private final double fAlpha;

	private final double fBeta;

	private final double fMu;

	private final IDistribution fU;

	public GeneralizedPareto(@Attribute("alpha") double alpha,
			@Attribute("beta") double beta,
			@Attribute(value = "mu", defaultValue = "0") double mu,
			@Attribute("UniformDistribution") IDistribution uniform) {
		fBeta = beta;
		fAlpha = alpha;
		fMu = mu;
		fU = uniform;
	}

	@Override
	public double sample() {
		return fMu + fBeta * (Math.pow(fU.sample(), -1.0 / fAlpha) - 1);
	}

	@Override
	public double expectation() {
		return fMu + fBeta / (fAlpha - 1.0);
	}

}
