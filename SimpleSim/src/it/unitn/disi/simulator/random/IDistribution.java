package it.unitn.disi.simulator.random;

/**
 * {@link IDistribution} represents a probability distribution.
 * 
 * @author giuliano
 */
public interface IDistribution {

	/**
	 * @return a number sampled from this distribution.
	 */
	public double sample();

	/**
	 * @return the expectation for this distribution.
	 */
	public double expectation();
}
