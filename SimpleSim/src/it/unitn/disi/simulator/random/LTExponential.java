package it.unitn.disi.simulator.random;

import peersim.config.Attribute;

/**
 * Left-truncated exponential distribution.
 *  
 * @author giuliano
 */
public class LTExponential extends Exponential {

	private final double fTruncate;

	public LTExponential(@Attribute("lambda") double lambda,
			@Attribute("truncation") double truncate,
			@Attribute("UniformDistribution") IDistribution uniform) {
		super(lambda, uniform);
		fTruncate = truncate;
	}

	@Override
	public double sample() {
		return fTruncate + super.sample();
	}

	@Override
	public double expectation() {
		return fTruncate + super.expectation();
	}
}
