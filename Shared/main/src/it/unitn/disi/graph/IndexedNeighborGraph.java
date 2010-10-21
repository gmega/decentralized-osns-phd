package it.unitn.disi.graph;

import peersim.graph.Graph;

public interface IndexedNeighborGraph extends Graph {
	public int getNeighbor(int nodeIndex, int neighborIndex);
}
