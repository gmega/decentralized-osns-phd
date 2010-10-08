package it.unitn.disi.sps.cyclon;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Linkable;
import peersim.core.Network;
import peersim.core.Node;
import peersim.extras.am.util.IncrementalStatsFreq;

@AutoConfig
public class CyclonSNMonitor implements Control {
	
	// ----------------------------------------------------------------------
	// Parameters.
	// ----------------------------------------------------------------------
	
	/**
	 * Protocol ID of {@link CyclonSN}.
	 */
	@Attribute
	private int protocol;
	
	/**
	 * Protocol ID of the one-hop social network.
	 */
	@Attribute("one_hop")
	private int onehop;
	
	// ----------------------------------------------------------------------
	// State.
	// ----------------------------------------------------------------------
	
	@Override
	public boolean execute() {
		IncrementalStatsFreq inDegree = new IncrementalStatsFreq(Network.size());
		for (int i = 0; i < Network.size(); i++) {
			Node node = Network.get(i);
			
			Linkable cyclon = (Linkable) node.getProtocol(protocol);
			int degree = cyclon.degree();
			for (int j = 0; j < degree; j++) {
				inDegree.add((int) cyclon.getNeighbor(j).getID());
			}
		}
		
		// Dumps the in-degrees.
		for (int i = 0; i < Network.size(); i++) {
			Node node = Network.get(i);
			Linkable sn = (Linkable) node.getProtocol(onehop);
			// [id], [social degree], [in-degree], [simulation time]
			System.out.println("INDG " + node.getID() + " " + sn.degree() + " "
					+ inDegree.getFreq((int) node.getID()) + " " + CommonState.getTime());
		}
		return false;
	}

}
