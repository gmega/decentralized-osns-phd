package it.unitn.disi.graph;

import peersim.graph.Graph;

/**
 * Extension to the {@link Graph} interface which supports retrieval of
 * neighbors by index.
 * 
 * @author giuliano
 */
public interface IndexedNeighborGraph extends Graph {
	/**
	 * Returns the k-th neighbor of the n-th node.
	 * 
	 * @param nodeIndex
	 *            the index of the node (should be smaller than
	 *            {@link Graph#size()}).
	 * @param neighborIndex
	 *            the index of the neighbor (should be smaller than
	 *            {@link Graph#degree(int)} for the selected node index).
	 * @return the corresponding neighbor.
	 * @throws ArrayIndexOutOfBoundsException
	 *             if boundaries are not respected.
	 */
	public int getNeighbor(int nodeIndex, int neighborIndex);
}
