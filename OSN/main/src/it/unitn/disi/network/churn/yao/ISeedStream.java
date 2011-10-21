package it.unitn.disi.network.churn.yao;

/**
 * An {@link ISeedStream} represents a stream of seeds for a random number
 * generator. It's in essence used to allow external clients to control the
 * behavior of the random number generators in Yao churn, through
 * {@link ScriptableGenerator}.
 * 
 * @author giuliano
 */
public interface ISeedStream {

	/**
	 * @return the next seed in the seed stream.
	 */
	public long nextSeed();

	/**
	 * @return whether the random number generator should be re-seeded or not.
	 */
	public boolean shouldReseed();

}
