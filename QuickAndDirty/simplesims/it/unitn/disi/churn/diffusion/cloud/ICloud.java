package it.unitn.disi.churn.diffusion.cloud;

import it.unitn.disi.churn.diffusion.HFloodMMsg;
import it.unitn.disi.utils.collections.Pair;

public interface ICloud {

	public static HFloodMMsg[] NO_UPDATE = new HFloodMMsg[] {};

	/**
	 * Writes an update to the profile page of a user.
	 * 
	 * @param page
	 *            id of the page (same as the id of the user).
	 * 
	 * @param update
	 *            the update to write.
	 */
	public void writeUpdate(int page, HFloodMMsg update);

	/**
	 * Fetches from the cloud all updates that are more recent than a given
	 * timestamp.
	 * 
	 * @param accessor
	 *            the node making the access.
	 * @param page
	 *            the page id for which to fetch updates.
	 * @param timestamp
	 *            a timestamp marking how old, at most, returned updates should
	 *            be.
	 * @return all updates with timestamp larger or equal to the query
	 *         timestamp.
	 */
	public HFloodMMsg[] fetchUpdates(int accessor, int page, double timestamp);

	/**
	 * Returns access statistics to the cloud.
	 * 
	 * @param id
	 *            id of the node for which we want to obtain statistics for.
	 * 
	 * @return a {@link Pair} containing the total number of accesses made by
	 *         the node, and the total number of <i>productive</i> accesses(i.e.
	 *         accesses that resulted in real updates being fetched) made by the
	 *         node.
	 */
	public Pair<Integer, Integer> accesses(int id);

	/**
	 * Resets the access counters for the cloud.
	 */
	public void resetAccessCounters();
}
