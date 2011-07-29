package it.unitn.disi.epidemics;

import peersim.config.IResolver;
import peersim.core.Linkable;
import peersim.core.Node;

/**
 * {@link NeighborhoodMulticast} sets as the destinations for a message the one
 * hop neighbors around {@link IGossipMessage#originator()} with respect to a
 * {@link Linkable}.
 * 
 * @author giuliano
 */
public class NeighborhoodMulticast implements IMessageVisibility {

	public static final String PAR_VISIBILITY_LINKABLE = "visibility_linkable";

	private final int fNeighborhoodId;

	public NeighborhoodMulticast(String prefix, IResolver resolver) {
		fNeighborhoodId = resolver.getInt(prefix, PAR_VISIBILITY_LINKABLE);
	}

	public NeighborhoodMulticast(int snId) {
		fNeighborhoodId = snId;
	}

	@Override
	public int size(IGossipMessage msg) {
		return neighborhood(msg).degree() + 1;
	}

	@Override
	public boolean isDestination(IGossipMessage msg, Node node) {
		return node.equals(center(msg)) || neighborhood(msg).contains(node);
	}

	@Override
	public Node get(IGossipMessage msg, int i) {
		if (i == 0) {
			return center(msg);
		}
		return (Node) neighborhood(msg).getNeighbor(i - 1);
	}
	
	private Linkable neighborhood(IGossipMessage msg) {
		return (Linkable) center(msg).getProtocol(fNeighborhoodId);
	}

	protected Node center(IGossipMessage msg) {
		return msg.originator();
	}

}
