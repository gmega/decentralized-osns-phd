package it.unitn.disi.f2f;

import java.util.BitSet;

import peersim.core.Linkable;
import it.unitn.disi.epidemics.IGossipMessage;

public interface IJoinListener {
	
	public void joinStarted(IGossipMessage message);
	
	public void descriptorsReceived(Linkable linkable, BitSet indices);
	
	public void joinDone(IGossipMessage message, int copies);
	
}
