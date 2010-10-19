package it.unitn.disi.sps;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

@AutoConfig
public class PerfectF2FPSBootstrap implements Control {

	@Attribute("protocol")
	private int fProtocol;

	@Override
	public boolean execute() {
		for (int i = 0; i < Network.size(); i++) {
			Node node = Network.get(i);
			PerfectF2FPeerSampling sps = (PerfectF2FPeerSampling) node
					.getProtocol(fProtocol);
			sps.init(node);
		}
		
		return false;
	}

}
