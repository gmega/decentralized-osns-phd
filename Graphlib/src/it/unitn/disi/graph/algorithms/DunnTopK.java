package it.unitn.disi.graph.algorithms;

import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.algorithms.GraphAlgorithms.IEdgeFilter;

import java.util.ArrayList;
import java.util.BitSet;

/**
 * Returns the top-k edge or vertex-disjoint least cost paths in a graph by
 * using an iterated application of Dijktra's algorithm. This is a heuristic
 * method not unlike <a href="http://dx.doi.org/10.1109/49.265708">Dunn's
 * heuristic</a>, and doesn't give guarantees on the aggregated cost of the
 * paths.
 * 
 * @author giuliano
 */
public class DunnTopK implements ITopKEstimator {

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

	public DunnTopK(IndexedNeighborGraph graph, double[][] weights, Mode mode) {
		fPrevious = new int[graph.size()];
		fMinDists = new double[graph.size()];
		fForbiddenVertices = new BitSet();
		fForbiddenEdges = new BitSet[graph.size()];
		
		fGraph = graph;
		fWeights = weights;
		fMode = mode;
		
		for (int i = 0; i < graph.size(); i++) {
			fForbiddenEdges[i] = new BitSet();
		}
	}

	public ArrayList<PathEntry> topKShortest(int source, int target, int k) {
		clearForbidden();
		ArrayList<PathEntry> paths = new ArrayList<PathEntry>();
		for (int i = 0; i < k; i++) {
			PathEntry entry = selectPath(source, target);
			if (entry == null) {
				break;
			}
			forbidPath(entry);
			paths.add(entry);
		}
		return paths;
	}

	public void clearForbidden() {
		fForbiddenVertices.clear();
		for (BitSet set : fForbiddenEdges) {
			set.clear();
		}
	}

	public double weight(int i, int j) {
		return fWeights[i][j];
	}

	private PathEntry selectPath(int source, int target) {
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

	private void forbidPath(PathEntry entry) {
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
