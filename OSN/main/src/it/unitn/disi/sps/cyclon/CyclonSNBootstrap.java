package it.unitn.disi.sps.cyclon;

import it.unitn.disi.utils.peersim.PermutingCache;
import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.Control;
import peersim.core.Linkable;
import peersim.core.Network;
import peersim.core.Node;

/**
 * Populates the CyclonSN view with random samples taken from a neighborhood
 * specified by another {@link Linkable}.
 * 
 * @author giuliano
 */
@AutoConfig
public class CyclonSNBootstrap implements Control {

	@Attribute
	private int protocol;

	@Attribute
	private int neighborhood;

	@Attribute
	private int size;

	@Override
	public boolean execute() {
		PermutingCache cache = new PermutingCache(neighborhood);

		for (int i = 0; i < Network.size(); i++) {
			Node node = Network.get(i);
			CyclonSN peerSampling = (CyclonSN) node.getProtocol(protocol);
			peerSampling.init(node);
			
			cache.populate(node);
			cache.shuffle();
			for (int j = 0; j < cache.size() && peerSampling.degree() < size; j++) {
				Node candidate = cache.get(j);
				if (canSelect(candidate)) {
					peerSampling.addNeighbor(candidate);
				}
			}
		}

		return false;
	}

	private boolean canSelect(Node candidate) {
		return true;
	}

}
