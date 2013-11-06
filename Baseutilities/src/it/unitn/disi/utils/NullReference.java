package it.unitn.disi.utils;

import it.unitn.disi.utils.tabular.IReference;
import peersim.core.Node;

public class NullReference<T> implements IReference<T>{

	@Override
	public T get(Node owner) {
		return null;
	}

}
