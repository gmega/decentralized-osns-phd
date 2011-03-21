package it.unitn.disi.random;

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
	
	public double expectation();
}
