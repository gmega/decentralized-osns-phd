package it.unitn.disi.utils;

import peersim.core.Node;

public class FallThroughReference<K> implements IReference<K> {
	
	private final K fReferred;
	
	public FallThroughReference(K referred) {
		fReferred = referred;
	}
	
	public K get(Node owner) {
		return fReferred;
	}
}
