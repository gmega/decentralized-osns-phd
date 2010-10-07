package it.unitn.disi.sps;

import java.util.BitSet;
import java.util.HashMap;

import example.sn.cyclon.CyclonSN;

import peersim.cdsim.CDProtocol;
import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.Linkable;
import peersim.core.Node;

/**
 * Simple protocol which tries to reconstruct the an underlying graph from
 * random samples picked up from the peer sampling service.
 * 
 * @author giuliano
 */
@AutoConfig
public class F2FOverlayCollector implements CDProtocol {

	@Attribute("social_neighbourhood")
	private int fStatic;

	@Attribute("sps")
	private int fSampled;

	private BitSet fSeen;

	public F2FOverlayCollector() {
	}
	
	@Override
	public void nextCycle(Node node, int protocolID) {
		Linkable statik = (Linkable) node.getProtocol(fStatic);
		CyclonSN sampled = (CyclonSN) node.getProtocol(fSampled);
		
		BitSet seen = seen(statik);

		for (int i = 0; i < sampled.degree(); i++) {
			Node neighbor = sampled.getNeighbor(i);
			if (statik.contains(neighbor)) {
				seen.set(i);
			}
		}
		
	}

	private BitSet seen(Linkable statik) {
		if (fSeen == null) {
			fSeen = new BitSet(statik.degree());
		}
		return fSeen;
	}

	public int achieved() {
		if (fSeen == null) {
			return -1;
		}
		return fSeen.cardinality();
	}
	
	public Object clone() {
		try {
			F2FOverlayCollector clone = (F2FOverlayCollector) super.clone();
			if (clone.fSeen != null) {
				clone.fSeen = new BitSet(fSeen.size());
				clone.fSeen.or(fSeen);
			}
			return clone;
		} catch(CloneNotSupportedException ex) {
			throw new RuntimeException(ex);
		}
	}
	
}
