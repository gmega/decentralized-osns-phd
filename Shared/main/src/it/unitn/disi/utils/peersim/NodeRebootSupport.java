package it.unitn.disi.utils.peersim;

import peersim.config.Configuration;
import peersim.core.Node;
import peersim.dynamics.NodeInitializer;

public class NodeRebootSupport {

	public static final String PAR_INIT = "init";

	private final NodeInitializer[] fInits;

	public NodeRebootSupport(String prefix) {
		Object[] tmp = Configuration.getInstanceArray(prefix + "." + PAR_INIT);
		fInits = new NodeInitializer[tmp.length];
		for (int i = 0; i < tmp.length; ++i) {
			fInits[i] = (NodeInitializer) tmp[i];
		}
	}

	public void initialize(Node node) {
		for (int j = 0; j < fInits.length; ++j) {
			runInitializer(node, fInits[j]);
		}
	}

	private void runInitializer(Node node, NodeInitializer initializer) {
		initializer.initialize(node);
	}
}
