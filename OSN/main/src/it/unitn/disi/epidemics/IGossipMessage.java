package it.unitn.disi.epidemics;

import peersim.core.Node;

/**
 * @author giuliano
 */
public interface IGossipMessage {
	/**
	 * @return the {@link Node} who originated this message.
	 */
	public Node originator();

	/**
	 * @return the local sequence number for this {@link IGossipMessage}.
	 *         Together with return value of {@link #originator()}, defines a
	 *         unique identifier for this message.
	 */
	public int sequenceNumber();

	/**
	 * @return the number of destinations for this {@link IGossipMessage}.
	 */
	public int destinations();

	/**
	 * @param i
	 *            an index value < {@link #destinations()}.
	 * @return the i-th destination for this {@link IGossipMessage}.
	 */
	public Node destination(int i);

	/**
	 * @param node
	 *            a {@link Node}.
	 * @return whether the node is a destination for this {@link IGossipMessage}
	 *         or not.
	 */
	public boolean isDestination(Node node);

	/**
	 * @return a payload object containing application-specific data for this
	 *         gossip message. Might be <code>null</code>.
	 */
	public Object payload();
}
