package it.unitn.disi.churn.diffusion.cloud;

import it.unitn.disi.churn.diffusion.HFloodMMsg;
import it.unitn.disi.simulator.core.ISimulationEngine;

public interface ICloud {

	public static HFloodMMsg[] NO_UPDATE = new HFloodMMsg[] {};

	/**
	 * Writes an update to the profile page of a user.
	 * 
	 * @param writer
	 *            id of the node performing the write.
	 * 
	 * @param page
	 *            id of the page (same as the id of the user).
	 * 
	 * @param update
	 *            the update to write.
	 */
	public void writeUpdate(int writer, int page, HFloodMMsg update,
			ISimulationEngine engine);

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
	public HFloodMMsg[] fetchUpdates(int accessor, int page, double timestamp,
			ISimulationEngine engine);

	/**
	 * Registers an access listener.
	 * 
	 * @param listener
	 *            the listener to be registered.
	 */
	public void addAccessListener(IAccessListener listener);

	public interface IAccessListener {
		/**
		 * Gets called back whenever someone accesses the cloud.
		 * 
		 * @param accessor
		 *            id of the node performing the access.
		 * @param page
		 *            id of the data object being accessed to.
		 * @param write
		 *            <code>true</code> if the access is a write or,
		 *            <code>false</code> if it's a read.
		 */
		public void registerAccess(int accessor, int page, AccessType type);
	}
	
	public enum AccessType {
		write,
		productive,
		nup
	}
}
