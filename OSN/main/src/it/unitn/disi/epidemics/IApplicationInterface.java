package it.unitn.disi.epidemics;

import it.unitn.disi.newscasting.ICachingObject;
import it.unitn.disi.utils.peersim.SNNode;
import peersim.core.Node;

/**
 * Application layer interface.
 * 
 * @author giuliano
 */
public interface IApplicationInterface extends ICachingObject {

	/**
	 * Delivers a message to the application layer. Called by the clients
	 * actually performing the message exchanges (e.g. the
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
	 *            should be set to a reference to the listener. Otherwise set it
	 *            to <code>null</code>.
	 * 
	 * @return <code>true</code> if the message has been delivered for the first
	 *         time, or <code>false</code> if it was a duplicate.
	 */
	public boolean deliver(SNNode sender, SNNode ours, IGossipMessage message,
			IEventObserver broadcaster);

	/**
	 * @return the PeerSim protocol id of the implementor.
	 */
	public int pid();

	/**
	 * @return a reference to the underlying {@link IEventStorage}.
	 */
	public IEventStorage storage();

	// ----------------------------------------------------------------------
	// Methods for subscribing to event notifications.
	// ----------------------------------------------------------------------
	/**
	 * Registers an observer to application events.
	 */
	public void addSubscriber(IEventObserver observer);

	/**
	 * Removes a previously registered observer to the application events, or
	 * does nothing if the supplied observer hadn't been previously registered.
	 */
	public void removeSubscriber(IEventObserver observer);
}
