package it.unitn.disi.graph.large.catalog;

import it.unitn.disi.graph.IndexedNeighborGraph;

public interface IAttributeComputer {
	public Number compute(IndexedNeighborGraph source, int root);
}
