package it.unitn.disi.sps;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;
import peersim.util.IncrementalStats;

@AutoConfig
public class CollectorMessageStatistics implements Control {

	@Attribute(value = "protocol")
	int fProtocolId;

	@Override
	public boolean execute() {
		for (int i = 0; i < Network.size(); i++) {
			Node node = Network.get(i);
			F2FOverlayCollector collector = (F2FOverlayCollector) node
					.getProtocol(fProtocolId);
			IncrementalStats stats = collector.receivedStats(); 
			StringBuffer str = new StringBuffer();
			str.append("M: ");
			str.append(node.getID());
			str.append(" ");
			str.append(stats.getMax());
			str.append(" ");
			str.append(stats.getMin());
			str.append(" ");
			str.append(stats.getAverage());
			
			System.err.println(str.toString());
		}
		
		return false;
	}

}
