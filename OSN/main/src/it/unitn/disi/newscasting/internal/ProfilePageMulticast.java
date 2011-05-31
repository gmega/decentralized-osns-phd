package it.unitn.disi.newscasting.internal;

import peersim.core.Node;
import it.unitn.disi.epidemics.IGossipMessage;
import it.unitn.disi.epidemics.NeighborhoodMulticast;
import it.unitn.disi.newscasting.Tweet;

public class ProfilePageMulticast extends NeighborhoodMulticast {

	public ProfilePageMulticast(int snId) {
		super(snId);
	}

	@Override
	protected Node center(IGossipMessage msg) {
		return ((Tweet) msg).profile();
	}
}
