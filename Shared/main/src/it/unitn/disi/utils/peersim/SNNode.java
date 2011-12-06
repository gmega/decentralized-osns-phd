package it.unitn.disi.utils.peersim;

import peersim.core.Node;

/**
 * Specialized {@link Node} interface.
 * 
 * @author giuliano
 */
public interface SNNode extends Node {

	/**
	 * @return the timestamp for the last state change for this {@link SNNode}.
	 */
	public long lastStateChange();

	/**
	 * @return the cumulative uptime for this {@link Node}, from the last call
	 *         to {@link #clearUptime()}, or from the begining of the simulation
	 *         if {@link #clearUptime()} hasn't been called.
	 */
	public long uptime(boolean includeCurrent);

	/**
	 * @return the cumulative downtime for this {@link Node}, from the last call
	 *         to {@link #clearDowntime()},, or from the begining of the
	 *         simulation if {@link #clearDowntime()} hasn't been called.
	 */
	public long downtime(boolean includeCurrent);

	/**
	 * @return the number of uptime segments for this node.
	 */
	public long uptimeN(boolean includeCurrent);

	/**
	 * @return the number of downtime segments for this node.
	 */
	public long downtimeN(boolean includeCurrent);

	/**
	 * Resets the uptime counter for this node.
	 */
	public void clearUptime();

	/**
	 * Resets the downtime counter for this node.
	 */
	public void clearDowntime();

	/**
	 * @param listener
	 *            sets an {@link INodeStateListener} for this {@link SNNode}.
	 *            For memory efficiency reasons, we take only one by default
	 *            (clients should build their own event broadcast bus if
	 *            needed).
	 */
	public void setStateListener(INodeStateListener listener);

	/**
	 * Clears the currently installed {@link INodeStateListener}, or does
	 * nothing if none is installed.
	 */
	public void clearStateListener();

	/**
	 * Sets the social network ID for this node.
	 * 
	 * @param id
	 *            the ID to be used for this node.
	 */
	public void setID(long id);

	/**
	 * @param id
	 *            the social network ID for this node.
	 */
	public void setSNId(int id);

	/**
	 * @return the social network ID for this node.
	 */
	public int getSNId();

	/*
	 * XXX these active/isActive methods aren't exactly great. I still got to
	 * properly decide whether to introduce unit experiment concepts or not, and
	 * where.
	 */
	/**
	 * 
	 * @return whether this {@link SNNode} is active from an
	 *         application-specific point of view or not. Protocols are free to
	 *         ignore this.
	 * 
	 */
	public boolean isActive();

	/**
	 * @param active
	 *            the active status for this node.
	 */
	public void active(boolean active);
}
