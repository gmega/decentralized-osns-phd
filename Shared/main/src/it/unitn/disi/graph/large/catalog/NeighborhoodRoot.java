package it.unitn.disi.graph.large.catalog;

import it.unitn.disi.graph.IndexedNeighborGraph;

public class NeighborhoodRoot implements ICatalogPart<Integer>{

	public static final String KEY = "root";
	
	@Override
	public String key() {
		return NeighborhoodRoot.KEY;
	}

	@Override
	public Integer compute(IndexedNeighborGraph g, int root) {
		return root;
	}

	@Override
	public Class<Integer> returnType() {
		return Integer.class;
	}

}
