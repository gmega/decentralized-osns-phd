package it.unitn.disi.newscasting.internal;

import it.unitn.disi.newscasting.IMessageVisibility;
import it.unitn.disi.newscasting.Tweet;
import peersim.core.Linkable;
import peersim.core.Node;

/**
 * {@link DefaultVisibility} means that a {@link Tweet} is to be addressed to
 * all friends of the owner of the profile to which it has been posted to. In
 * other words, it's to be addressed to the social neighorhood of the
 * {@link Node} returned by {@link Tweet#profile()}.
 * 
 * @author giuliano
 */
public class DefaultVisibility implements IMessageVisibility {

	private final int fSnId;

	public DefaultVisibility(int snId) {
		fSnId = snId;
	}

	@Override
	public int size(Tweet tweet) {
		return socialNetwork(tweet).degree();
	}

	@Override
	public boolean isDestination(Tweet tweet, Node node) {
		return socialNetwork(tweet).contains(node);
	}

	@Override
	public Node get(Tweet tweet, int i) {
		return socialNetwork(tweet).getNeighbor(i);
	}

	private Linkable socialNetwork(Tweet tweet) {
		return (Linkable) tweet.profile().getProtocol(fSnId);
	}
}
