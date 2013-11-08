package it.unitn.disi.graph.large.catalog;

public 	class CatalogRecord {
	
	final int root;
	final int size;
	final double clustering;
	final long offset;

	public CatalogRecord(ICatalogCursor source) {
		root = source.get("root").intValue();
		size = source.get("size").intValue();
		clustering = source.get("clustering").doubleValue();
		offset = source.get("offset").longValue();
	}
}