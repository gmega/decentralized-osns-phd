package it.unitn.disi.graph.analysis;

import java.util.BitSet;

import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;
import it.unitn.disi.utils.DisjointSets;
import peersim.graph.Graph;

public class GraphAlgorithms {

	public static boolean isConnected(Graph g) {
		final DisjointSets set = new DisjointSets(g.size());

		for (int i = 0; i < g.size(); i++) {
			for (int neighbor : g.getNeighbours(i)) {
				int setI = set.find(i);
				int setN = set.find(neighbor);
				if (setI != setN) {
					set.union(setI, setN);
				}
			}
		}

		int setId = set.find(0);
		for (int i = 1; i < g.size(); i++) {
			if (setId != set.find(i)) {
				return false;
			}
		}

		return true;
	}

	public static boolean isDirected(IndexedNeighborGraph g) {
		for (int i = 0; i < g.size(); i++) {
			for (int j = 0; j < g.degree(i); j++) {
				int neighbor = g.getNeighbor(i, j);
				if (!g.isEdge(neighbor, i)) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean isSimple(IndexedNeighborGraph g) {
		BitSet set = new BitSet(g.size());
		for (int i = 0; i < g.size(); i++) {
			set.clear();
			set.set(i);
			for (int j = 0; j < g.degree(i); j++) {
				int neighbor = g.getNeighbor(i, j);
				if (set.get(neighbor)) {
					return false;
				}
				set.set(neighbor);
			}
		}
		return true;
	}

	public static int countTriads(LightweightStaticGraph g, int root) {
		if (g.directed() || !g.isSimple()) {
			throw new IllegalArgumentException();
		}
		// Assumes the graph is undirected and simple.
		int triads = 0;
		int degree = g.degree(root);
		for (int i = 0; i < degree; i++) {
			for (int j = i; j < degree; j++) {
				if (g.isEdge(i, j)) {
					triads++;
				}
			}
		}

		return triads;
	}

}
