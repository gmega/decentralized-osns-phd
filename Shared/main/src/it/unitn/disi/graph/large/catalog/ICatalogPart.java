package it.unitn.disi.graph.large.catalog;

import it.unitn.disi.graph.IndexedNeighborGraph;

/**
 * Neighborhood catalogs are made of {@link ICatalogPart}s, which are
 * essentially attributes which can computed over neighborhoods.
 * 
 * @author giuliano
 * @param <T>
 */
public interface ICatalogPart<T extends Number> {

	/**
	 * @return a {@link String} identifier for this {@link ICatalogPart}.
	 *         Ideally those should be unique.
	 */
	public String key();

	/**
	 * @return a {@link Class} representing the type of this attribute.
	 */
	public Class<T> returnType();

	/**
	 * Computes the attribute value over a neighborhood.
	 * 
	 * @param g
	 *            the input graph.
	 * 
	 * @param root
	 *            the root of the neighborhood.
	 * 
	 * @return the attribute value.
	 */
	public T compute(IndexedNeighborGraph g, int root);

}
