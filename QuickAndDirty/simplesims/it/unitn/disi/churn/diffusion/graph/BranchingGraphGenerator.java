package it.unitn.disi.churn.diffusion.graph;

import peersim.graph.BitMatrixGraph;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;

public class BranchingGraphGenerator {

	public IndexedNeighborGraph branchingGraph(int paths, int[] nodesPerPath) {
		int destination = sum(nodesPerPath) + 1;
		BitMatrixGraph graph = new BitMatrixGraph(destination + 1);

		int next = 1;
		for (int i = 0; i < paths; i++) {

			// Wires the ends of the graph.
			graph.setEdge(0, next);
			graph.setEdge(nodesPerPath[i], destination);

			// Wires inside of the path.
			for (int j = 0; j < (nodesPerPath[i] - 1); j++) {
				graph.setEdge(next + j, next + j + 1);
			}

			next += nodesPerPath[i];
		}

		return LightweightStaticGraph.undirect(LightweightStaticGraph
				.fromGraph(graph));
	}

	private int sum(int[] nodesPerPath) {
		int sum = 0;
		for (int i = 0; i < nodesPerPath.length; i++) {
			sum += nodesPerPath[i];
		}
		return sum;
	}

}
