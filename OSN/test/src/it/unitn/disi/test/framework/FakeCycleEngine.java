package it.unitn.disi.test.framework;

import java.util.Collection;
import java.util.Properties;

import peersim.cdsim.CDProtocol;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Node;
import peersim.core.Protocol;

public class FakeCycleEngine {
	
	private final Node [] fNetwork;
	
	private final long fRndSeed;
	
	public FakeCycleEngine(Collection<Node> network, long rndSeed) {
		fNetwork = network.toArray(new Node[network.size()]);
		fRndSeed = rndSeed;
		Configuration.setConfig(new Properties());
	}
	
	public void run(int cycles) {
		CommonState.setEndTime(cycles);
		CommonState.initializeRandom(fRndSeed);
		for (int i = 0; i < cycles; i++) {
			CommonState.setTime((long)i);
			runExperiment();
		}
	}
	
	private void runExperiment() {
		for (int j = 0; j < fNetwork.length; j++) {
			Node node = fNetwork[j];
			int protos = node.protocolSize();
			for (int k = 0; k < protos; k++) {
				Protocol p = node.getProtocol(k);
				if (p instanceof CDProtocol) {
					CommonState.setNode(node);
					CommonState.setPid(k);
					((CDProtocol)p).nextCycle(node, k);
				}
			}
		}
	}
}
