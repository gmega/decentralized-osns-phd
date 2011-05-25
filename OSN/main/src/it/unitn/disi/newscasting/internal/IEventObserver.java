package it.unitn.disi.newscasting.internal;

import it.unitn.disi.epidemics.IGossipMessage;
import it.unitn.disi.utils.peersim.SNNode;

/**
 * {@link IEventObserver} observer is an interface for communicating events that
 * are relevant to the parts of the {@link SocialNewscastingService}.
 * 
 * @author giuliano
 */
public interface IEventObserver {

	/**
	 * Caller has produced some content locally. Semantically equivalent to have
	 * the application delivering a message to itself.
	 */
	public void localDelivered(IGossipMessage message);

	/**
	 * Application has received a message.
	 * 
	 * @param sender
	 *            Node sending the message.
	 * @param receiver
	 *            Node receiving the message (caller node). Has to be different
	 *            from the sender.
	 * @param message
	 *            the actual message.
	 * @param duplicate
	 *            <code>true</code> if the received message was a duplicate, or
	 *            <code>false</code> otherwise.
	 */
	public void delivered(SNNode sender, SNNode receiver,
			IGossipMessage message, boolean duplicate);
}
