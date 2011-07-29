package it.unitn.disi.epidemics;

import peersim.core.Node;

public interface IPushContentExchangeStrategy extends IContentExchangeStrategy {

	/**
	 * Asks the {@link IPushContentExchangeStrategy} to schedule the next update
	 * it would like to disseminate, taking into account that some neighbors
	 * might be disallowed by the selection filter.<BR>
	 * 
	 * @param filter
	 * @return
	 */
	public IGossipMessage scheduleNext(Node node, ISelectionFilter filter);

}
