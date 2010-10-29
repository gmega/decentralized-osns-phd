package it.unitn.disi.network;

import it.unitn.disi.application.SimpleTrafficGenerator;
import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

@AutoConfig
public class AllQuiet implements Control{
	
	@Attribute
	int application;
	
	public AllQuiet() { }

	public boolean execute() {
		int size = Network.size();
		
		for (int i = 0; i < size; i++) {
			Node node = Network.get(i);
			SimpleTrafficGenerator app = (SimpleTrafficGenerator) node.getProtocol(application);
			app.suppressTweeting(true);
		}
		
		return false;
	}
}
