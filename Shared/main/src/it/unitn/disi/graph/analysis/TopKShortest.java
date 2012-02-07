package it.unitn.disi.graph.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.PriorityQueue;

import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.analysis.GraphAlgorithms.IEdgeFilter;

/**
 * {@link TopKShortest} computes the <i>k</i> shortest paths between a source
 * and a destination. The class implements a variant of <a
 * href="http://www.jstor.org/stable/2629312"> Yen's algorithm</a>, most notably
 * using Lawler's optimizations as described by <a
 * href="http://citeseerx.ist.psu.edu/viewdoc/summary?doi=10.1.1.54.3286"
 * >Brander and Sinclair</a>.
 * 
 * @author giuliano
 */
public class TopKShortest {

	private IndexedNeighborGraph fGraph;

	private double[] fMinDists;

	private double[][] fWeights;

	private BitSet[] fBranchedEdges;

	private BitSet fCurrentRoot;

	private int[] fPrevious;

	private IEdgeFilter fFilter = new IEdgeFilter() {
		@Override
		public boolean isForbidden(int i, int j) {
			return fCurrentRoot.get(j) || fBranchedEdges[i].get(j);
		}
	};

	public TopKShortest(IndexedNeighborGraph graph, double[][] weights) {
		fGraph = graph;
		fWeights = weights;
		fBranchedEdges = new BitSet[graph.size()];
		fPrevious = new int[graph.size()];
		fMinDists = new double[graph.size()];
		fCurrentRoot = new BitSet();
		for (int i = 0; i < graph.size(); i++) {
			fBranchedEdges[i] = new BitSet();
		}
	}

	public ArrayList<PathEntry> topKShortest(int source, int target, int k) {
		ArrayList<PathEntry> paths = new ArrayList<PathEntry>();
		PriorityQueue<PathEntry> queue = new PriorityQueue<PathEntry>();

		// Produces the shortest path.
		paths.add(initialPath(source, target));

		while (true) {
			// Takes the (paths.size() - 1)st shortest path.
			PathEntry kMinusOne = paths.get(paths.size() - 1);

			// Keeps track of how many "ties" we have at the top of the
			// heap. The main problem here is that we cannot efficiently ask
			// that to the heap later, so we need to compute it as we go.
			double kthCost = Double.POSITIVE_INFINITY;
			int kthCostCount = 0;
			if (!queue.isEmpty()) {
				kthCost = queue.peek().cost;
				kthCostCount = 1;
			}

			// Generates and solves the subproblems.
			for (int i = kMinusOne.spurIndex; i < (kMinusOne.path.length - 1); i++) {
				PathEntry path = branch(paths, i, target, kMinusOne);
				if (path != null) {
					if (kthCost == path.cost) {
						kthCostCount++;
					} else if (path.cost < kthCost) {
						kthCost = path.cost;
						kthCostCount = 1;
					}
					queue.add(path);
				}
			}

			// Empty queue means we cannot produce anymore paths, so we're done.
			if (queue.isEmpty()) {
				System.err.println("EMPTY.");
				break;
			}

			// Whatever is at the top of the queue, that's our paths.size()-th
			// longest path.
			// If we have enough tying paths to stop the algorithm, then stop
			// it.
			if (kthCostCount + paths.size() >= k) {
				while (paths.size() < k) {
					paths.add(queue.remove());
				}

				// done.
				break;
			}

			// Otherwise pick an arbitrary path to branch next.
			paths.add(queue.remove());
		}

		return paths;
	}

	private PathEntry branch(ArrayList<PathEntry> paths, int deviation,
			int target, PathEntry kMinusOne) {

		// Wipe clean the BitSets.
		fCurrentRoot.clear();
		for (BitSet bset : fBranchedEdges) {
			bset.clear();
		}

		// The main difficulty with Yen's and Lawler's algorithm is to
		// filter out the edges and vertexes which are not allowed in
		// the next spur. To do that, we must:
		//
		// 1. Filter out vertices in the root.
		for (int j = 0; j <= deviation; j++) {
			fCurrentRoot.set(kMinusOne.path[j]);
		}

		// 2. For the current branch vertex, filter out the edges that
		// have already been followed by previous deviations from this vertex,
		// and that share the same root as this new spur.
		//
		// XXX I should probably keep a prefix tree with all the k paths. That
		// would make things substantially more efficient.
		int branchVertex = kMinusOne.path[deviation];

		for (PathEntry entry : paths) {
			int branched = entry.branchingVertex(kMinusOne, deviation);
			// Paths don't branch here, so just proceed.
			if (branched == -1) {
				continue;
			}
			// Paths branch here, need to exclude the edge.
			fBranchedEdges[branchVertex].set(branched);
		}

		// Finally, computes the spur.
		GraphAlgorithms.dijkstra(fGraph, fFilter, branchVertex, fWeights,
				fMinDists, fPrevious);

		// Reconstructs the paths from the reverse tree returned by
		// Dijkstra's algorithm.
		int size = GraphAlgorithms.dijkstraPathSize(fPrevious, target);
		if (size == -1) {
			return null;
		}

		// For the new path ...
		int[] path = new int[size + deviation];
		// ... prepend the root ...
		System.arraycopy(kMinusOne.path, 0, path, 0, deviation + 1);
		// ... and then the spur.
		GraphAlgorithms.dijkstraPath(fPrevious, target, path, path.length - 1);

		// Finally computes the cost.
		double cost = 0.0;
		for (int i = 0; i < (path.length - 1); i++) {
			cost += fWeights[path[i]][path[i + 1]];
		}

		return new PathEntry(path, deviation, cost);
	}

	private PathEntry initialPath(int source, int destination) {
		GraphAlgorithms
				.dijkstra(fGraph, source, fWeights, fMinDists, fPrevious);
		int size = GraphAlgorithms.dijkstraPathSize(fPrevious, destination);
		if (size == -1) {
			return null;
		}

		int[] path = new int[size];
		GraphAlgorithms.dijkstraPath(fPrevious, destination, path,
				path.length - 1);

		return new PathEntry(path, 0, fMinDists[destination]);
	}

	public static class PathEntry implements Comparable<PathEntry> {

		public int[] path;
		public final int spurIndex;
		public final double cost;

		private final int hash;

		private PathEntry(int[] path, int spurIndex, double cost) {
			this.path = path;
			this.spurIndex = spurIndex;
			this.cost = cost;

			int hash = 0;
			for (int pathElement : path) {
				hash = hash + 17 * pathElement;
			}
			this.hash = hash;
		}

		/**
		 * Checks whether this {@link PathEntry} shares a common prefix with
		 * another {@link PathEntry} up until a certain index vertex. In case
		 * positive, returns the vertex followed by this path, which the first
		 * vertex in which it differs from the path passed as parameter. If the
		 * two paths don't share a prefix until the index node, returns -1.
		 * 
		 * @param other
		 * @param index
		 * @return
		 */
		public int branchingVertex(PathEntry other, int index) {
			if (path.length - 1 <= index) {
				return -1;
			}

			for (int i = 0; i <= index; i++) {
				if (other.path[i] != this.path[i]) {
					// The paths don't share a common prefix.
					return -1;
				}
			}

			return path[index + 1];
		}

		@Override
		public int compareTo(PathEntry o) {
			return (int) Math.signum(this.cost - o.cost);
		}

		@Override
		public int hashCode() {
			return hash;
		}

		@Override
		public String toString() {
			return Arrays.toString(path);
		}
	}
}
