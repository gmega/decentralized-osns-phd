package it.unitn.disi.newscasting.internal;

import it.unitn.disi.epidemics.IGossipMessage;
import it.unitn.disi.newscasting.IMessageVisibility;
import it.unitn.disi.newscasting.Tweet;
import peersim.config.IResolver;
import peersim.core.Linkable;
import peersim.core.Node;

/**
 * {@link SocialNeighborhoodMulticast} means that a {@link Tweet} is addressed
 * to an entire social neighborhood.
 * 
 * @author giuliano
 */
public class SocialNeighborhoodMulticast implements IMessageVisibility {

	public static final String PAR_VISIBILITY_LINKABLE = "visibility_linkable";

	private final int fNeighborhoodId;

	public SocialNeighborhoodMulticast(String prefix, IResolver resolver) {
		fNeighborhoodId = resolver.getInt(prefix, PAR_VISIBILITY_LINKABLE);
	}

	public SocialNeighborhoodMulticast(int snId) {
		fNeighborhoodId = snId;
	}

	@Override
	public int size(IGossipMessage msg) {
		return neighborhood(cast(msg)).degree() + 1;
	}

	@Override
	public boolean isDestination(IGossipMessage msg, Node node) {
		return node.equals(cast(msg).profile())
				|| neighborhood(cast(msg)).contains(node);
	}

	@Override
	public Node get(IGossipMessage msg, int i) {
		if (i == 0) {
			return cast(msg).profile();
		}
		return (Node) neighborhood(cast(msg)).getNeighbor(i - 1);
	}

	private Linkable neighborhood(IGossipMessage msg) {
		return (Linkable) msg.originator().getProtocol(fNeighborhoodId);
	}
	
	private Tweet cast(IGossipMessage msg) {
		return (Tweet) msg;
	}
}
