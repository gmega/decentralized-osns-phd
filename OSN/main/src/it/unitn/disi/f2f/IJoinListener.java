package it.unitn.disi.f2f;

import it.unitn.disi.epidemics.IGossipMessage;

import java.util.BitSet;

import peersim.core.Linkable;

public interface IJoinListener {
	
	public void joinStarted(IGossipMessage message);
	
	public void descriptorsReceived(Linkable linkable, BitSet indices);
	
	public boolean joinDone(IGossipMessage starting, JoinTracker tracker);
	
}
