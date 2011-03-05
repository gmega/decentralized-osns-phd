package it.unitn.disi.newscasting;

import peersim.core.Node;

/**
 * {@link IMessageVisibility} can tell the intended destinations of a
 * {@link Tweet}.<BR>
 * <BR>
 * <b>Performance note:</b> Implementations should strive to be efficient for
 * repeated accesses with the same {@link Tweet} object. Clients, in turn,
 * should strive to not interleave calls with different {@link Tweet} objects.
 * 
 * @author giuliano
 */
public interface IMessageVisibility {
	/**
	 * @return the number of destinations for a {@link Tweet}. That includes the
	 *         node posting the message.
	 */
	public int size(Tweet tweet);

	/**
	 * @return the i-th destination for a {@link Tweet} (among the nodes
	 *         returned, the poster of the message).
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if i >= {@link #size(Tweet)}.
	 */
	public Node get(Tweet tweet, int i);

	/**
	 * Tells whether a {@link Node} is in the destination list for a given
	 * {@link Tweet}. Again, # 
	 * 
	 * @return <code>true</code> if the tweet is in the destination list, or
	 *         <code>false</code> otherwise.
	 */
	public boolean isDestination(Tweet tweet, Node node);
}
