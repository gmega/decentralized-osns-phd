package it.unitn.disi.f2f;

import it.unitn.disi.epidemics.IGossipMessage;

public interface IJoinListener {
	
	public void joinStarted(IGossipMessage message);
	
	public void joinDone(IGossipMessage message, int copies);
	
}
