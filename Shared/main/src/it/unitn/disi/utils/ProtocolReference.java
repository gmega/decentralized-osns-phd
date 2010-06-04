package it.unitn.disi.utils;

import peersim.core.Node;

public class ProtocolReference<K> implements IReference<K>{

	private final int fReferredId;
	
	public ProtocolReference(int id){
		fReferredId = id;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public K get(Node owner) {
		return (K) owner.getProtocol(fReferredId);
	}

}
