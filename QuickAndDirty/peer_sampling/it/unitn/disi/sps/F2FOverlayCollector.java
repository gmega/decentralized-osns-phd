package it.unitn.disi.sps;

import it.unitn.disi.sps.cyclon.CyclonSN;
import it.unitn.disi.utils.peersim.PeersimUtils;
import it.unitn.disi.utils.peersim.SharedBuffer;

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

	private static final SharedBuffer<Integer> fIndexes = new SharedBuffer<Integer>(
			0);
	
	enum Mode {
		RANDOM, PEERSAMPLING, COLLECTOR;
	}

	@Attribute("social_neighbourhood")
	private int fStatic;

	@Attribute("sps")
	private int fSampled;

	private Mode fMode;

	private BitSet fSeen;

	private int fCasualHits;

	private int fProactiveHits;
	
	public F2FOverlayCollector(@Attribute("mode") String mode) {
		fMode = Mode.valueOf(mode.toUpperCase());
	}

	@Override
	public void nextCycle(Node node, int protocolID) {
		Linkable statik = (Linkable) node.getProtocol(fStatic);
		CyclonSN sampled = (CyclonSN) node.getProtocol(fSampled);

		bitsetInit(statik);
		resetCounters();

		// Collect friends from the peer sampling layer (if any).
		collectFriends(statik, sampled);

		// Proactively tries to obtain new entries.
		if (fMode != Mode.RANDOM) {
			huntForIPs(statik, sampled, protocolID);
		}
	}

	private void collectFriends(Linkable statik, CyclonSN sampled) {
		for (int i = 0; i < statik.degree(); i++) {
			Node neighbor = statik.getNeighbor(i);
			if (sampled.contains(neighbor)) {
				fCasualHits++;
				fSeen.set(i);
			}
		}
	}

	private void huntForIPs(Linkable ourSn, CyclonSN sampled, int protocolID) {
		int sampledDegree = sampled.degree();

		// From the nodes we have in our peer sampling view...
		for (int i = 0; i < sampledDegree; i++) {
			Node neighbor = sampled.getNeighbor(i);
			Linkable neighborSn = (Linkable) neighbor.getProtocol(fStatic);

			// ... if the guy doesn't share unseen neighbors with us at the
			// social network level, don't even bother.
			if (intersectingUnseenIndex(ourSn, neighborSn) == -1) {
				continue;
			}

			int index = -1;
			// If he does, "contacts" the neighbor and queries for a useful IP.
			// Contact strategy depends on the mode.
			switch (fMode) {
			case PEERSAMPLING:
				index = queryPSLayer(ourSn, neighbor);
				break;
			case COLLECTOR:
				index = queryCollectorLayer(ourSn, neighbor, protocolID);
				break;
			}

			// Found something.
			if (index != -1) {
				fSeen.set(index);
				fProactiveHits++;
			}

			// Either way we stop. If index was -1 the contact failed and
			// we wasted it.
			break;
		}
	}

	private int queryPSLayer(Linkable ourSn, Node neighbor) {
		return intersectingUnseenIndex(ourSn,
				(Linkable) neighbor.getProtocol(fSampled));
	}

	private int queryCollectorLayer(Linkable ourSn, Node neighbor,
			int protocolID) {
		F2FOverlayCollector collector = (F2FOverlayCollector) neighbor
				.getProtocol(protocolID);
		Linkable neighborSn = (Linkable) neighbor.getProtocol(fStatic);
		BitSet seen = collector.bitsetInit(neighborSn);

		// From the set of neighbors seen by our neighbor...
		for (int i = seen.nextSetBit(0); i >= 0; i = seen.nextSetBit(i + 1)) {
			Node candidate = neighborSn.getNeighbor(i);
			// ... sees whether it is a friend of ours as well ...
			if (ourSn.contains(candidate)) {
				// ... and we haven't seen it yet.
				int idx = PeersimUtils.indexOf(candidate, ourSn);
				if (!fSeen.get(idx)) {
					return idx;
				}
			}
		}

		return -1;
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

	private BitSet bitsetInit(Linkable statik) {
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
