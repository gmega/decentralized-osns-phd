package it.unitn.disi.epidemics;

import it.unitn.disi.utils.peersim.SNNode;
import peersim.core.Node;

/**
 * Generic interface for a content exchange operation between two nodes. It
 * assumes that the content exchange strategy and the peer selection strategy
 * are provided by different objects (or by one single object providing both
 * interfaces).
 * 
 * @author giuliano
 */
public interface IContentExchangeStrategy extends ICachingObject {

	/**
	 * Performs the exchange.
	 * 
	 * @param source
	 *            the peer initiating the exchange.
	 * @param peer
	 *            the peer selected for exchange.
	 * 
	 * @return <code>true</code> if the exchange was successful, or
	 *         <code>false</code> otherwise.
	 * 
	 */
	public boolean doExchange(SNNode source, SNNode peer);

	/**
	 * @return the activity status for this protocol.
	 * 
	 * @see ActivityStatus
	 */
	public ActivityStatus status();

	enum ActivityStatus {
		/**
		 * Means the protocol still has messages to send.
		 */
		ACTIVE,

		/**
		 * Means that no matter which peer is selected,
		 * {@link IContentExchangeStrategy#doExchange(Node, Node)} won't result
		 * in any messages being exchanged.
		 */
		QUIESCENT,

		/**
		 * Means that the protocol is non-terminating.
		 */
		PERPETUAL
	}
}
