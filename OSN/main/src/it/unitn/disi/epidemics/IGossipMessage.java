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
	 * Called when this message gets forwarded to another node.
	 */
	public void forwarded(Node from, Node to);
	
	/**
	 * Called when this message gets dropped by the current node.
	 */
	public void dropped(Node at);

	/**
	 * A message can be "flyweighted" if its content is immutable. This means a
	 * protocol that is forwarding this object to another node can simply send a
	 * reference to this message object.<BR>
	 * <BR>
	 * If instead this method returns <code>false</code>, it means the message
	 * object has to be <code>cloned</code> before it is forwarded.
	 * 
	 * @return
	 */
	public boolean canFlyweight();

	/**
	 * @return a clone of this message (always, even if it can be flyweighted a
	 *         new object will be returned).
	 */
	public Object clone();

	/**
	 * Convenience method which will clone this message if
	 * {@link #canFlyweight()} returns <code>true</code>, or return an instance
	 * to itself if it returns false.
	 */
	public IGossipMessage cloneIfNeeded();
}
