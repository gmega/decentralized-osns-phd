package it.unitn.disi.graph.large.catalog;

import it.unitn.disi.graph.IndexedNeighborGraph;

public class NeighborhoodRoot implements ICatalogPart<Integer>{

	@Override
	public String key() {
		return "root";
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
