package it.unitn.disi.churn.diffusion.graph;

import peersim.graph.BitMatrixGraph;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.analysis.PathEntry;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;
import it.unitn.disi.utils.collections.Pair;

public class BranchingGraphGenerator {

	/**
	 * Given a set of overlapping paths, "unfolds" them by constructing a graph
	 * with vertex-disjoint paths only. Resulting graphs always have the
	 * following structure:<BR>
	 * 
	 * {@code
	 *   o--o--o--o
	 *  /          \
	 * o--o--o--o---o
	 *  \          /
	 *   o--o--o--o
	 * } And nodes that are shared among paths get replicated.
	 * 
	 * @param paths
	 *            the set of paths for the graph.
	 * @return
	 */
	public static Pair<IndexedNeighborGraph, int[]> branchingGraph(
			PathEntry[] paths) {
		int count = sum(paths);
		int[] map = new int[count];

		BitMatrixGraph graph = new BitMatrixGraph(count, false);

		// 0 and 1 are source and destination.
		int[] path = paths[0].path;
		map[0] = path[0];
		map[1] = path[path.length - 1];

		int mapped = 1;

		for (PathEntry entry : paths) {
			path = entry.path;

			// Special case for 1 edge path.
			if (path.length == 2) {
				graph.setEdge(0, 1);
				graph.setEdge(1, 0);
				continue;
			}

			// Wires source.
			map[++mapped] = path[1];
			graph.setEdge(0, mapped);

			// Wires intermediate vertices.
			for (int i = 2; i < (path.length - 1); i++) {
				map[++mapped] = path[i];
				graph.setEdge(mapped - 1, mapped);
			}

			// Wires destination.
			graph.setEdge(mapped, 1);
		}

		return new Pair<IndexedNeighborGraph, int[]>(
				LightweightStaticGraph.fromGraph(graph), map);
	}

	private static int sum(PathEntry[] entries) {
		int sum = 0;
		for (PathEntry entry : entries) {
			sum += (entry.path.length - 2);
		}

		return sum + 2;
	}

}
