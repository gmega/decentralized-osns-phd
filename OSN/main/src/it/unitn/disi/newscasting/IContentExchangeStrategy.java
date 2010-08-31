package it.unitn.disi.newscasting;

import peersim.core.Node;

/**
 * Generic interface for a content exchange operation between two nodes. It
 * assumes that the content exchange strategy and the peer selection strategy
 * are provided by different objects (or by one single object providing both
 * interfaces).
 * 
 * @author giuliano
 */
public interface IContentExchangeStrategy {
	
	/**
	 * Performs the exchange.
	 * 
	 * @param source
	 *            the peer initiating the exchange.
	 * @param peer
	 *            the peer selected for exchange.
	 * 
	 * @return <code>true</code> if the exchange indeed happen, or
	 *         <code>false</code> otherwise. Exchanges may fail to happen for a
	 *         number of reasons, for example if the selected peer refuses a
	 *         connection.
	 * 
	 */
	public boolean doExchange(Node source, Node peer);
	
	/**
	 * @return an optimization hint which might allow the caller to speed up
	 *         diffusion at a low cost. The meaning is implementation-specific,
	 *         but it is a hint as to how many times the
	 *         {@link #doExchange(Node, Node)} method should be called
	 *         per-round.<BR>
	 *         <BR>
	 *         If the implementor does not care about throttling, then it should
	 *         return 1.
	 */
	public int throttling(Node node);
	
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
