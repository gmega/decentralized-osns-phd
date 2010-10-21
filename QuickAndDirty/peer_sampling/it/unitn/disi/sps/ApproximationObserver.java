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
		
		int randomHits = 0;
		int proactiveHits = 0;
		
		for (int i = 0; i < Network.size(); i++) {
			Node node = Network.get(i);
			F2FOverlayCollector collector = (F2FOverlayCollector) node
					.getProtocol(fPid);
			
			randomHits += collector.randomHits();
			proactiveHits += collector.proactiveHits();
			
			built += collector.achieved();
			total += ((Linkable) node.getProtocol(fSnPid)).degree();
		}
		
		int totalHits = randomHits + proactiveHits;
		
		StringBuffer buffer = new StringBuffer();
		
		buffer.append("ApproximationObserver: ");
		buffer.append(built);
		buffer.append(" ");
		buffer.append(total);
		buffer.append(" ");
		buffer.append(safeDivide(randomHits, totalHits));
		buffer.append(" ");
		buffer.append(safeDivide(proactiveHits, totalHits));
		
		System.out.println(buffer);
		return built == total;
	}
	
	private String safeDivide(int numerator, int denominator) {
		if (denominator == 0) {
			return "NaN";
		}
		
		return Double.toString(((double)numerator)/denominator);
	}
}
