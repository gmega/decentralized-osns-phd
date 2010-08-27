package it.unitn.disi.sps.debug;

import it.unitn.disi.utils.MultiCounter;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Linkable;
import peersim.core.Network;
import peersim.core.Node;

public class SampleTracker implements Control {
	
	private static final String PAR_PROTOCOL = "protocol";
	
	private static final String PAR_NODE_ID = "nodeid";
	
	private final long fNodeId;
	
	private final int fProtocolId;
	
	private MultiCounter<Node> fCounter = new MultiCounter<Node>();
	
	public SampleTracker(String prefix) {
		fProtocolId = Configuration.getPid(prefix + "." + PAR_PROTOCOL);
		fNodeId = Configuration.getLong(prefix + "." + PAR_NODE_ID);
	}

	public boolean execute() {
		
		Node node = null;
		for(int i = 0; i < Network.size(); i++) {
			Node candidate = Network.get(i); 
			if (candidate.getID() == fNodeId) {
				node = candidate;
				break;
			}
		}
		
		if (node == null) {
			return false;
		}
				
		Linkable neighborhood = (Linkable) node.getProtocol(fProtocolId);
		for (int i = 0; i < neighborhood.degree(); i++) {
			fCounter.increment(neighborhood.getNeighbor(i));
		}

		// Performs a dump at the last round.
		if (CommonState.getTime() == (CommonState.getEndTime() - 1)) {
			for (Node key : fCounter) {
				System.out.println(key.getID() + " " + fCounter.hist(key));
			}
		}
		
		return false;
	}

}
 