package it.unitn.disi.sps;

import it.unitn.disi.sps.cyclon.CyclonSN;

import java.util.BitSet;

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

	private int fCasualHits;

	private int fProactiveHits;

	public F2FOverlayCollector() {
	}

	@Override
	public void nextCycle(Node node, int protocolID) {
		Linkable statik = (Linkable) node.getProtocol(fStatic);
		CyclonSN sampled = (CyclonSN) node.getProtocol(fSampled);
		resetCounters();
		
		// Collect friends from the peer sampling layer (if any).
		collectFriends(statik, sampled);

		// Proactively tries to obtain new entries.
		huntForIPs(statik, sampled);
	}

	private void collectFriends(Linkable statik, CyclonSN sampled) {
		BitSet seen = seen(statik);
		for (int i = 0; i < statik.degree(); i++) {
			Node neighbor = statik.getNeighbor(i);
			if (sampled.contains(neighbor)) {
				fCasualHits++;
				seen.set(i);
			}
		}
	}

	private void huntForIPs(Linkable ourSn, CyclonSN sampled) {
		int sampledDegree = sampled.degree();
		for (int i = 0; i < sampledDegree; i++) {
			Node neighbor = sampled.getNeighbor(i);
			// Checks if this guy shares friends with us
			// that we haven't yet seen.
			Linkable neighborSn = (Linkable) neighbor.getProtocol(fStatic);
			if (intersectingUnseenIndex(ourSn, neighborSn) != -1) {
				// "Contacts" the neighbor and queries for a useful IP.
				int index = intersectingUnseenIndex(ourSn, (Linkable) neighbor
						.getProtocol(fSampled));
				// Found something.
				if (index != -1) {
					fSeen.set(index);
					fProactiveHits++;
				}
				
				break;
			}
		}
	}

	private int intersectingUnseenIndex(Linkable ourSn, Linkable another) {
		int degree = ourSn.degree();
		for (int i = 0; i < degree; i++) {
			Node candidate = ourSn.getNeighbor(i);
			if (another.contains(candidate) && !fSeen.get(i)) {
				return i;
			}
		}

		return -1;
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
	
	public int randomHits() {
		return fCasualHits;
	}
	
	public int proactiveHits() {
		return fProactiveHits;
	}

	private void resetCounters() {
		fCasualHits = fProactiveHits = 0;
	}

	public Object clone() {
		try {
			F2FOverlayCollector clone = (F2FOverlayCollector) super.clone();
			if (clone.fSeen != null) {
				clone.fSeen = new BitSet(fSeen.size());
				clone.fSeen.or(fSeen);
			}
			return clone;
		} catch (CloneNotSupportedException ex) {
			throw new RuntimeException(ex);
		}
	}

}
