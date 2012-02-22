package it.unitn.disi.graph.analysis;

import java.util.ArrayList;
import java.util.BitSet;

import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.analysis.GraphAlgorithms.IEdgeFilter;

public class TopKShortestDisjoint {

	public static enum Mode {
		VertexDisjoint, EdgeDisjoint
	}

	private IndexedNeighborGraph fGraph;

	private double[] fMinDists;

	private double[][] fWeights;

	private BitSet[] fForbiddenEdges;

	private BitSet fForbiddenVertices;

	private Mode fMode;

	private int[] fPrevious;

	private IEdgeFilter fFilter = new IEdgeFilter() {
		@Override
		public boolean isForbidden(int i, int j) {
			return fForbiddenVertices.get(j) || fForbiddenEdges[i].get(j);
		}
	};

	public TopKShortestDisjoint(IndexedNeighborGraph graph, double[][] weights,
			Mode mode) {
		fGraph = graph;
		fPrevious = new int[graph.size()];
		fMinDists = new double[graph.size()];
		fWeights = weights;
		fMode = mode;
		fForbiddenVertices = new BitSet();
		fForbiddenEdges = new BitSet[graph.size()];
		for (int i = 0; i < graph.size(); i++) {
			fForbiddenEdges[i] = new BitSet();
		}
	}

	public ArrayList<PathEntry> topKShortest(int source, int target, int k) {
		ArrayList<PathEntry> paths = new ArrayList<PathEntry>();
		for (int i = 0; i < k; i++) {
			PathEntry entry = nextPath(source, target);
			if (entry == null) {
				break;
			}
			forbid(entry);
			paths.add(entry);
		}
		return paths;
	}

	private PathEntry nextPath(int source, int target) {
		GraphAlgorithms.dijkstra(fGraph, fFilter, source, fWeights, fMinDists,
				fPrevious);

		int size = GraphAlgorithms.dijkstraPathSize(fPrevious, target);
		if (size == -1) {
			return null;
		}

		int[] path = new int[size];
		GraphAlgorithms.dijkstraPath(fPrevious, target, path, path.length - 1);

		return new PathEntry(path, fMinDists[target]);
	}

	private void forbid(PathEntry entry) {
		int[] path = entry.path;
		switch (fMode) {
		case EdgeDisjoint:
			for (int i = 0; i < (path.length - 1); i++) {
				fForbiddenEdges[path[i]].set(path[i + 1]);
			}
			break;
		case VertexDisjoint:
			for (int i = 1; i < (path.length - 1); i++) {
				fForbiddenVertices.set(path[i]);
			}
			break;
		}
	}
}
