package it.unitn.disi.f2f;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.Network;
import peersim.core.Node;

@AutoConfig
public class F2FOverlayCollectorInitializer implements peersim.core.Control{
	
	@Attribute
	int linkable;

	@Override
	public boolean execute() {
		for (int i = 0; i < Network.size(); i++) {
			Node node = Network.get(i);
			F2FMaintainer collector = (F2FMaintainer)node.getProtocol(linkable);
			collector.init(node);
		}
		
		return false;
	}

}
