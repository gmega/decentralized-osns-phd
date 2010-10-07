package it.unitn.disi.sps.cyclon;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
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
		System.out.println("BEGIN_INDEGREEDUMP");
		for (int i = 0; i < inDegree.getMax(); i++) {
			System.out.println(i + " " + inDegree.getFreq(i));
		}
		System.out.println("END_INDEGREEDUMP");
		
		return false;
	}

}
