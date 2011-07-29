package it.unitn.disi.epidemics;

import peersim.core.Node;

/**
 * An {@link IPushPeerSelector} is an extended {@link IPeerSelector} which can
 * be parameterized by the update that has been currently scheduled.
 * 
 * @author giuliano
 */
public interface IPushPeerSelector extends IPeerSelector {
	public Node selectPeer(Node source, ISelectionFilter filter,
			IGossipMessage message);
}
