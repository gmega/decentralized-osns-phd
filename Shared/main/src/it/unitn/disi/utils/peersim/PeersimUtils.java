package it.unitn.disi.utils.peersim;

import peersim.config.FastConfig;
import peersim.core.Network;
import peersim.core.Node;
import peersim.util.IncrementalStats;

public class PeersimUtils {
	
	public static Object getLinkable(Node ourNode, int protocolID,
			int linkableID) {
		return ourNode.getProtocol(convertId(protocolID, linkableID));
	}
	
	public static int convertId(int protocolID, int linkableID) {
		return FastConfig.getLinkable(protocolID, linkableID);
	}
	
	public static Node lookupNode(long id) {
		for (int i = 0; i < Network.size(); i++) {
			Node candidate = Network.get(i);
			if (candidate.getID() == id) {
				return candidate;
			}
		}
		
		return null;
	}
	
	public static void print(String label, IncrementalStats stats) {
		StringBuffer buf = new StringBuffer();
		buf.append(label);
		buf.append(": ");
		buf.append(stats.getMin());
		buf.append(" ");
		buf.append(stats.getMax());
		buf.append(" ");
		buf.append(stats.getAverage());
		buf.append(" ");
		buf.append(stats.getVar());
		System.out.println(buf);
	}
}
