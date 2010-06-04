package it.unitn.disi.application;

import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

public class BiasedComponentSelectorInitializer implements Control{
	
	private static final String PAR_BCS = "bcsid";
	
	private int fLinkableId;
	
	public BiasedComponentSelectorInitializer(String name) {
		fLinkableId = Configuration.getPid(name + "." + PAR_BCS); 
	}

	public boolean execute() {
		for (int i = 0; i < Network.size(); i++) {
			bootstrapNode(Network.get(i));
		}
		
		return false;
	}

	private void bootstrapNode(Node node) {
		BiasedComponentSelector selector = (BiasedComponentSelector) node.getProtocol(fLinkableId);
		selector.updateSelection(node);
	}

}
