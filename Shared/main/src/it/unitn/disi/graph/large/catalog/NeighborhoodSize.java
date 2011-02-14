package it.unitn.disi.graph.large.catalog;

import it.unitn.disi.graph.IndexedNeighborGraph;

public class NeighborhoodSize implements ICatalogPart<Integer>{

	@Override
	public String key() {
		return "size";
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
