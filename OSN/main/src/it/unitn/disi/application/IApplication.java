package it.unitn.disi.application;

import peersim.core.Node;
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
	 * @param caller
	 *            Calling of this method will trigger the firing of either
	 *            {@link IEventObserver#duplicateReceived(Node, Node, Node, int, int)}
	 *            or of
	 *            {@link IEventObserver#eventDelivered(Node, Node, Node, int, int)}
	 *            . Since the caller of might be listening to these events
	 *            himself, he might want to be excluded from the notification.
	 *            In this case, the caller should pass a reference to his
	 *            {@link IEventObserver} in this parameter.
	 * 
	 * @param sender
	 *            the node sending the tweet.
	 * @param receiver
	 *            the node receiving the tweet.
	 * @param tweet
	 *            the tweet.
	 * 
	 * @return <code>true</code> if this message was not a duplicate, or
	 *         <code>false</code> otherwise.
	 */
	public boolean receiveTweet(IEventObserver caller, Node sender,
			Node receiver, Tweet tweet);

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
}
