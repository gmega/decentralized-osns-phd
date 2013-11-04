package it.unitn.disi.graph.large.catalog;

import it.unitn.disi.graph.IndexedNeighborGraph;

public class NeighborhoodSize implements ICatalogPart<Integer>{

	public static final String KEY = "size";
	
	@Override
	public String key() {
		return KEY;
	}

	@Override
	public Integer compute(IndexedNeighborGraph g, int root) {
		return g.degree(root);
	}

	@Override
	public Class<Integer> returnType() {
		return Integer.class;
	}

}
