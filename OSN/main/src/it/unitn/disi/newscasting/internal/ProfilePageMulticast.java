package it.unitn.disi.newscasting.internal;

import it.unitn.disi.epidemics.IGossipMessage;
import it.unitn.disi.epidemics.NeighborhoodMulticast;
import it.unitn.disi.newscasting.Tweet;
import peersim.core.Node;

public class ProfilePageMulticast extends NeighborhoodMulticast {

	public ProfilePageMulticast(int snId) {
		super(snId);
	}

	@Override
	protected Node center(IGossipMessage msg) {
		return ((Tweet) msg).profile();
	}
}
