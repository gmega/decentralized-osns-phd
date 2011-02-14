package it.unitn.disi.graph.large.catalog;

import it.unitn.disi.graph.IndexedNeighborGraph;

public interface ICatalogPart<T extends Number> {
	
	public String key();
	
	public T compute(IndexedNeighborGraph g, int root);
	
	public Class<T> returnType(); 
	
}
