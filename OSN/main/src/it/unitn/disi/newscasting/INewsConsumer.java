package it.unitn.disi.newscasting;

import peersim.core.Node;

public interface INewsConsumer {
	
	public void eventsReceived(Node sender, Node receiver, int start, int end);
}
