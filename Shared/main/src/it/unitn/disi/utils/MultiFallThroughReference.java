package it.unitn.disi.utils;

import java.util.HashMap;
import java.util.Map;

import peersim.core.Node;

public class MultiFallThroughReference<K> implements IReference<K> {
	private final Map<Node, K> fReferred = new HashMap<Node, K>();
	
	public MultiFallThroughReference(Node [] nodes, K [] objects) {
		for (int i = 0; i < nodes.length; i++) {
			fReferred.put(nodes[i], objects[i]);
		}
	}

	@Override
	public K get(Node owner) {
		return fReferred.get(owner);
	}
}
