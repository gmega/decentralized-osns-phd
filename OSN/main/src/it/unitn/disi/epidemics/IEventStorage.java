package it.unitn.disi.epidemics;

import it.unitn.disi.newscasting.Tweet;

import java.util.Iterator;
import java.util.Set;

import peersim.core.Node;

/**
 * {@link IEventStorage} stores {@link IGossipMessage} instances.
 * 
 * XXX Some confusion between messages and content here. This is mainly because
 * we send around the content object themselves.
 * 
 * @author giuliano
 */
public interface IEventStorage {
	/**
	 * @param msg
	 *            a message.
	 * 
	 * @return <code>true</code> if the application already received this
	 *         {@link Tweet}, or <code>false</code> otherwise.
	 */
	public boolean contains(IGossipMessage msg);

	/**
	 * Retrieves an {@link IGossipMessage} under the given key.
	 * 
	 * @param originator
	 *            the node originating the message
	 * @param sequence
	 *            the message's sequence number
	 * @return
	 */
	public IGossipMessage retrieve(Node originator, int sequence);

	/**
	 * @return the number of distinct {@link IGossipMessage} owners being kept
	 *         by this event storage.
	 */
	public int distinctNodes();

	/**
	 * @return a read-only {@link Set} with the nodes for which this
	 *         {@link IEventStorage} has tweets for.
	 */
	public Set<Node> nodes();

	/**
	 * @return an {@link Iterator} to the known tweets for a given node.
	 */
	public Iterator<IGossipMessage> tweetsFor(Node node);

	/**
	 * @return the number of elements that have been stored.
	 */
	public int elements();
}
