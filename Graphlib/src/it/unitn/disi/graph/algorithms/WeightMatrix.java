package it.unitn.disi.graph.algorithms;

/**
 * {@link WeightMatrix} provides a simple interface for accessing the weights
 * associated to edges in a graph. The main goal is allowing freedom in choosing
 * the representation for the matrix (as in using an algorithm to generate
 * weights, or a other memory-efficient representations).
 * 
 * @author giuliano
 */
public interface WeightMatrix {
	public double get(int i, int j);

	public double get(int i, int j, int index);
}
