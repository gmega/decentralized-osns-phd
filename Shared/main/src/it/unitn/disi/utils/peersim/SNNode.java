package it.unitn.disi.utils.peersim;

import peersim.core.Node;

public interface SNNode extends Node {
	
	public long uptime();
	
	public long downtime();
	
	public void clearUptime();
	
	public void clearDowntime();
}
