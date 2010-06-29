package it.unitn.disi.application;

import peersim.core.Node;
import it.unitn.disi.IAdaptable;
import it.unitn.disi.application.interfaces.IEventObserver;

/**
 * Application-level interface.
 * 
 * @author giuliano
 */
public interface IApplication extends IAdaptable {

	/**
	 * Attempts to deliver tweet to the application level of the receiver node.
	 * 
	 * @param sender
	 *            the node sending the tweet.
	 * @param receiver
	 *            the node receiving the tweet.
	 * @param tweet
	 *            the tweet.
	 * @param caller
	 *            Calling of this method will trigger the firing of either
	 *            {@link IEventObserver#duplicateReceived(Node, Node, Node, int, int)}
	 *            or of
	 *            {@link IEventObserver#eventDelivered(Node, Node, Node, int, int)}
	 *            . Since the caller of be listening to these events himself, he
	 *            might want to be excluded from the notification. In this case,
	 *            the caller should pass a reference to his
	 *            {@link IEventObserver} in this parameter.
	 * @return <code>true</code> if this message was not a duplicate, or
	 *         <code>false</code> otherwise.
	 */
	public boolean receiveTweet(Node sender, Node receiver,
			Tweet tweet, IEventObserver caller);

	/**
	 * @param tweet
	 *            a tweet.
	 * 
	 * @return <code>true</code> if the application already received this
	 *         {@link Tweet}, or <code>false</code> otherwise.
	 */
	public boolean knows(Tweet tweet);

	/**
	 * Same as {@link #knows(Tweet)}, except that the tweet is represented as a
	 * {@link Node}, {@link Integer} pair instead of a {@link Tweet} object.
	 */
	public boolean knows(Node node, int sequence);
	
	/**
	 * Register an {@link IEventObserver} to application-level events.
	 */
	public void addSubscriber(IEventObserver observer);

	/**
	 * Suppresses or unsuppresses tweeting by this node.
	 * 
	 * @return <code>true</code> if suppressing was previously toggled, or
	 *         <code>false</code> otherwise.
	 */
	public boolean toggleTweeting();

	/**
	 * @return <code>true</code> if this node is currently suppressing tweets,
	 *         or <code>false</code> otherwise.
	 */ 
	public boolean isSuppressingTweets();
	
	/**
	 * @return the amount of tweets that the application would still have to
	 *         receive in order to have complete knowledge about all produced
	 *         tweets.
	 */
	public int pendingReceives();

	/**
	 * Sets the traffic generator's seed to a given value. Useful for generating
	 * predictable traffic patterns.
	 * 
	 * @param seed
	 *            the seed to be used.
	 */
	public void setTrafficGeneratorSeed(long seed);
}
