package it.unitn.disi.newscasting;

import java.util.Iterator;
import java.util.Set;

import peersim.core.Node;

/**
 * {@link IEventStorage} stores {@link Tweet} instances.
 * 
 * @author giuliano
 */
public interface IEventStorage {
	/**
	 * @param tweet
	 *            a tweet.
	 * 
	 * @return <code>true</code> if the application already received this
	 *         {@link Tweet}, or <code>false</code> otherwise.
	 */
	public boolean contains(Tweet tweet);
	
	/**
	 * @return the number of distinct {@link Tweet} owners being kept by this
	 *         event storage.
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
	public Iterator<Tweet> tweetsFor(Node node);

	/**
	 * @return the number of elements that have been stored.
	 */
	public int elements();
}
