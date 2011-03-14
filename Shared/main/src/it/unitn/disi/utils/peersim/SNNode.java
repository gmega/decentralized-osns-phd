package it.unitn.disi.utils.peersim;

import peersim.core.Node;

/**
 * Specialized {@link Node} interface.
 * 
 * @author giuliano
 */
public interface SNNode extends Node {

	/**
	 * @return the cumulative uptime for this {@link Node}, from the last call
	 *         to {@link #clearUptime()}, or from the begining of the simulation
	 *         if {@link #clearUptime()} hasn't been called.
	 */
	public long uptime();

	/**
	 * @return the cumulative downtime for this {@link Node}, from the last call
	 *         to {@link #clearDowntime()},, or from the begining of the
	 *         simulation if {@link #clearDowntime()} hasn't been called.
	 */
	public long downtime();

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

}
