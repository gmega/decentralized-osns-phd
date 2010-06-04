package it.unitn.disi.application.interfaces;

import peersim.core.Node;

/**
 * {@link IEventObserver} observer is an interface for communicating events that
 * are relevant to the parts of the {@link NewscastApplication}.
 * 
 * @author giuliano
 * 
 */
public interface IEventObserver {

	/**
	 * Caller has tweeted.
	 * 
	 * @param owner
	 *            Tweeting node.
	 * 
	 * @param sequenceNumber
	 *            Sequence number of the tweet.
	 */
	public void tweeted(Node owner, int sequenceNumber);

	/**
	 * Caller has received a tweet (or tweet range). Note that this method
	 * should be called <b>only if the tweet is being received for the first
	 * time.</b> For duplicates, the caller should use
	 * {@link IEventObserver#duplicateReceived(Node, Node, Node, int, int)}.
	 * 
	 * @param sender
	 *            Node sending the tweet.
	 * @param receiver
	 *            Node receiving the tweet (caller node).
	 * @param owner
	 *            Node originating the tweet.
	 * @param start
	 *            Start of sequence number range.
	 * @param end
	 *            End of sequence number range.
	 */
	public void eventDelivered(Node sender, Node receiver, Node owner,
			int start, int end);

	/**
	 * Caller has received a tweet, but it was a duplicate. Parameters are the
	 * same as in {@link #eventDelivered(Node, Node, Node, int, int)}.
	 */
	// XXX we could simply add a flag to {@link #eventDelivered(Node, Node, Node, int, int)}.
	public void duplicateReceived(Node sender, Node receiver, Node owner,
			int start, int end);

}
