package it.unitn.disi.newscasting.internal;

import peersim.core.Node;
import it.unitn.disi.IAdaptable;
import it.unitn.disi.newscasting.IApplicationInterface;
import it.unitn.disi.newscasting.IContentExchangeStrategy;
import it.unitn.disi.newscasting.Tweet;

/**
 * Internal interface to the social newscasting service. Meant for communication
 * between exchange strategies and the core service, as well as between
 * instances of the {@link ICoreInterface}.
 * 
 * @author giuliano
 */
public interface ICoreInterface extends IApplicationInterface, IAdaptable {

	/**
	 * Notifies the social newscasting service that a message has been received.
	 * Called by the clients actually performing the message exchanges (i.e. the
	 * {@link IContentExchangeStrategy} instances). <BR>
	 * Calls to this method will trigger:<BR>
	 * 
	 * <ol>
	 * <li> {@link IEventObserver#eventDelivered(Node, Node, Node, int, int)} if
	 * the message has been delivered for the first time;</li>
	 * <li> {@link IEventObserver#duplicateReceived(Node, Node, Node, int, int)}
	 * if the message is a duplicate.</li>
	 * </ol>
	 * 
	 * @param sender
	 *            the node sending the message.
	 * @param ours
	 *            the node owning this instance of the soc. newscasting service;
	 *            i.e., the node receiving the message.
	 * @param tweet
	 *            the actual message.
	 * @param broadcaster
	 *            if the client has a registered listener through
	 *            {@link #addSubscriber(IEventObserver)} and wants to exclude
	 *            this listener from receiving the events, then this parameter
	 *            should be set to a reference to the listener.
	 * 
	 * @return <code>true</code> if the message has been delivered for the first
	 *         time, or <code>false</code> if it was a duplicate.
	 */
	public boolean receiveTweet(Node sender, Node ours, Tweet tweet,
			IEventObserver broadcaster);

	/**
	 * Registers an observer to the newscasting service events.
	 * 
	 * @param observer
	 */
	public void addSubscriber(IEventObserver observer);

	/**
	 * @return the number of contacts performed to this node from the last call
	 *         to {@link #resetCounters()}.
	 */
	public int contacts();

	/**
	 * @return the number of messages pending receive from the last call to
	 *         {@link #resetCounters()}.
	 */
	public int pendingReceives();

	/**
	 * Resets all statistic counters.
	 */
	public void resetCounters();

}
