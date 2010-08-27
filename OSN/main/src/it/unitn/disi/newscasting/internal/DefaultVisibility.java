package it.unitn.disi.newscasting.internal;

import peersim.core.Linkable;
import peersim.core.Node;
import it.unitn.disi.newscasting.IMessageVisibility;
import it.unitn.disi.newscasting.Tweet;


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
