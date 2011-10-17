package it.unitn.disi.utils.peersim;

import peersim.config.Configuration;
import peersim.config.FastConfig;
import peersim.core.Linkable;
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
	
	public static int selfPid(String prefix) {
		int idx = prefix.lastIndexOf('.');
		idx = (idx == -1) ? 0 : idx;
		String name = prefix.substring(idx + 1);
		return Configuration.lookupPid(name);
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
	
	public static int countActives() {
		int active = 0;
		for (int i = 0; i < Network.size(); i++) {
			if (Network.get(i).isUp()) {
				active++;
			}
		}	
		return active;
	}
	
	public static int indexOf(Node node, Linkable linkable) {
		int degree = linkable.degree();
		for (int i = 0; i < degree; i++) {
			if (node.equals(linkable.getNeighbor(i))) {
				return i;
			}
		}
		return -1;
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
