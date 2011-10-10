package it.unitn.disi.unitsim;

import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;

/**
 * {@link INeighborhoodProvider} is a base service allowing piecewise access to
 * an underlying, possibly huge graph, without having to load it into memory.
 * 
 * @author giuliano
 */
public interface INeighborhoodProvider {

	/**
	 * @return the number of vertices in the underlying graph, or -1 if that
	 *         cannot be established beforehand.
	 */
	public int size();

	/**
	 * The n-th neighborhood of the underlying graph, as a subgraph. Node IDs
	 * are mapped from zero to {@link LightweightStaticGraph#size()}. The
	 * original IDs for the vertices can be recovered by calling
	 * {@link #verticesOf(Integer)}.
	 * 
	 * @param node
	 * @return
	 */
	public abstract IndexedNeighborGraph neighborhood(Integer node);

	/**
	 * The neighbors of a vertex in the graph, with their IDs in the ID space of
	 * the underlying graph.
	 * 
	 * @param node
	 *            the id of the vertex, in the ID space of the underlying graph.
	 * @return its neighbors in the underlying graph.
	 */
	public abstract int[] verticesOf(Integer node);

}