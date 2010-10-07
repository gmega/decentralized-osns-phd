package it.unitn.disi.sps;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.Control;
import peersim.core.Linkable;
import peersim.core.Network;
import peersim.core.Node;

/**
 * Tells how far we are from building the actual social network.
 * 
 * @author giuliano
 */
@AutoConfig
public class ApproximationObserver implements Control {
	
	@Attribute("protocol")
	int fPid;
	
	@Attribute("social_neighbourhood")
	int fSnPid;

	@Override
	public boolean execute() {
		int built = 0;
		int total = 0;
		
		for (int i = 0; i < Network.size(); i++) {
			Node node = Network.get(i);
			F2FOverlayCollector collector = (F2FOverlayCollector) node
					.getProtocol(fPid);
			built += collector.achieved();
			total += ((Linkable) node.getProtocol(fSnPid)).degree();
		}
		
		System.out.println("ApproximationObserver: " + built + " " + total);
		return false;
	}
}
