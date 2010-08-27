package it.unitn.disi.newscasting.internal;

import it.unitn.disi.newscasting.Tweet;
import peersim.core.Node;

/**
 * {@link IEventObserver} observer is an interface for communicating events that
 * are relevant to the parts of the {@link SocialNewscastingService}.
 * 
 * @author giuliano
 * 
 */
public interface IEventObserver {

	/**
	 * Caller has tweeted.
	 */
	public void tweeted(Tweet tweet);

	/**
	 * Caller has received a tweet.
	 * 
	 * @param sender
	 *            Node sending the tweet.
	 * @param receiver
	 *            Node receiving the tweet (caller node).
	 * @param tweet
	 *            the actual message.
	 * @param duplicate
	 *            <code>true</code> if the received message was a duplicate, or
	 *            <code>false</code> otherwise.
	 */
	public void eventDelivered(Node sender, Node receiver, Tweet tweet,
			boolean duplicate);
}
