package it.unitn.disi.sps;

import it.unitn.disi.utils.peersim.PermutingCache;
import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.Control;
import peersim.core.Linkable;
import peersim.core.Network;
import peersim.core.Node;
import peersim.dynamics.NodeInitializer;

/**
 * Populates a {@link Linkable} with random samples taken from a neighborhood
 * specified by another {@link Linkable}.
 * 
 * @author giuliano
 */
@AutoConfig
public class SNBootstrap implements Control, NodeInitializer {
	
	@Attribute
	private int protocol;

	@Attribute
	private int neighborhood;

	@Attribute
	private int size;
	
	private PermutingCache fCache;

	@Override
	public boolean execute() {
		for (int i = 0; i < Network.size(); i++) {
			Node node = Network.get(i);
			initialize(node);
		}

		return false;
	}
	
	@Override
	public void initialize(Node node) {
		PermutingCache cache = cache();
		Linkable peerSampling = (Linkable) node.getProtocol(protocol);
		this.preInit(node, peerSampling);
		cache.populate(node);
		cache.shuffle();

		for (int j = 0; j < cache.size() && peerSampling.degree() < size; j++) {
			Node candidate = cache.get(j);
			if (canSelect(candidate)) {
				peerSampling.addNeighbor(candidate);
			}
		}
		
		this.postInit(node, peerSampling);
	}
	
	protected void preInit(Node node, Linkable peerSampling) {
		
	}

	protected boolean canSelect(Node candidate) {
		return true;
	}
	
	protected void postInit(Node node, Linkable peersampling) {
		
	}
	
	private PermutingCache cache() {
		if (fCache == null) {
			fCache = new PermutingCache(neighborhood);
		}
		return fCache;
	}
}
