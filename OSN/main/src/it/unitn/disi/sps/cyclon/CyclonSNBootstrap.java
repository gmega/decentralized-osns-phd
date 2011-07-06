package it.unitn.disi.sps.cyclon;

import it.unitn.disi.sps.SNBootstrap;
import peersim.config.AutoConfig;
import peersim.core.Linkable;
import peersim.core.Node;

@AutoConfig
public class CyclonSNBootstrap extends SNBootstrap {
	@Override
	protected void preInit(Node node, Linkable peerSampling) {
		((CyclonSN) peerSampling).init(node);
	}
	
	@Override
	public boolean canSelect(Node node) {
		return node.isUp();
	}
}
