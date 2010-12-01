package it.unitn.disi.graph.analysis;

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

}
