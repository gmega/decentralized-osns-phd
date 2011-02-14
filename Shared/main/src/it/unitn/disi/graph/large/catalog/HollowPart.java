package it.unitn.disi.graph.large.catalog;

import it.unitn.disi.graph.IndexedNeighborGraph;

public class HollowPart<T extends Number> implements ICatalogPart<T> {

	private final String fKey;
	
	private final Class<T> fKlass;
	
	public HollowPart(Class<T> klass, String key) {
		fKlass = klass;
		fKey = key;
	}
	
	@Override
	public String key() {
		return fKey;
	}

	@Override
	public T compute(IndexedNeighborGraph g, int root) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Class<T> returnType() {
		return fKlass;
	}

}
