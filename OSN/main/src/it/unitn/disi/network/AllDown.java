package it.unitn.disi.network;

import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

/**
 * Simple initializer which brings the entire network down.
 * 
 * @author giuliano
 */
public class AllDown implements Control {
	
	public AllDown(String s) { }
	
	@Override
	public boolean execute() {
		int size = Network.size();
		for (int i = 0; i < size; i++) {
			Node node = Network.get(i);
			node.setFailState(Node.DOWN);
		}
		
		return false;
	}

}
