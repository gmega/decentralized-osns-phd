package it.unitn.disi.utils.graph;

import peersim.graph.Graph;

public class GraphGenerator {
	public static void wireFull(Graph g) {
		for (int i = 0; i < g.size(); i++) {
			for (int j = i; j < g.size(); j++) {
				g.setEdge(i, j);
				if (g.directed()) {
					g.setEdge(j, i);
				}
			}
		}
	}
}
