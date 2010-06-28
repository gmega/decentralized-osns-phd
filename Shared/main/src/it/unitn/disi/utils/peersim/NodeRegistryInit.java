package it.unitn.disi.utils.peersim;

import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;
import peersim.dynamics.NodeInitializer;

public class NodeRegistryInit implements Control, NodeInitializer {

	public NodeRegistryInit(String s) {}
	
	@Override
	public boolean execute() {
		for (int i = 0; i < Network.size(); i++) {
			initialize(Network.get(i));
		}
		
		return false;
	}

	@Override
	public void initialize(Node n) {
		NodeRegistry.getInstance().registerNode(n);
	}

}
