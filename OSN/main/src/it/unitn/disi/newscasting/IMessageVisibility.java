package it.unitn.disi.newscasting;

import it.unitn.disi.epidemics.IGossipMessage;
import peersim.core.Node;

/**
 * {@link IMessageVisibility} can tell the intended destinations of a
 * {@link IGossipMessage}.<BR>
 * <BR>
 * <b>Performance note:</b> Implementations should strive to be efficient for
 * repeated accesses with the same {@link IGossipMessage} object. Clients, in
 * turn, should strive to not interleave calls with different
 * {@link IGossipMessage} objects.
 * 
 * @author giuliano
 */
public interface IMessageVisibility {
	/**
	 * @return the number of destinations for a {@link IGossipMessage}. That
	 *         includes the node posting the message.
	 */
	public int size(IGossipMessage msg);

	/**
	 * @return the i-th destination for a {@link IGossipMessage} (among the
	 *         nodes returned, the poster of the message).
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if i >= {@link #size(Tweet)}.
	 */
	public Node get(IGossipMessage msg, int i);

	/**
	 * Tells whether a {@link Node} is in the destination list for a given
	 * {@link IGossipMessage}.
	 * 
	 * @return <code>true</code> if the tweet is in the destination list, or
	 *         <code>false</code> otherwise.
	 */
	public boolean isDestination(IGossipMessage msg, Node node);
}
