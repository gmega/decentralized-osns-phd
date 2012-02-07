package it.unitn.disi.graph.analysis;

import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;
import it.unitn.disi.utils.streams.DisjointSets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.PriorityQueue;

import peersim.graph.Graph;

public class GraphAlgorithms {

	// --------------------------------------------------------------------------

	public static boolean isConnected(Graph g) {
		DisjointSets set = computeComponents(g);
		int setId = set.find(0);
		for (int i = 1; i < g.size(); i++) {
			if (setId != set.find(i)) {
				return false;
			}
		}
		return true;
	}

	// --------------------------------------------------------------------------

	public static int[] components(Graph graph) {
		DisjointSets sets = GraphAlgorithms.computeComponents(graph);
		int[] components = new int[graph.size()];
		for (int i = 0; i < graph.size(); i++) {
			components[sets.find(i)]++;
		}
		return components;
	}

	// --------------------------------------------------------------------------

	private static DisjointSets computeComponents(Graph g) {
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
		return set;
	}

	// --------------------------------------------------------------------------
	/**
	 * Answers whether a graph emulates an undirected graph or not, by asking
	 * whether for every arc there's a corresponding reverse one. Note that the
	 * answer is only reliable <b>if the graph is simple</b>.
	 */
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

	// --------------------------------------------------------------------------
	/**
	 * Same as {@link #isDirected(Graph)}, but for graphs which do not support
	 * the {@link IndexedNeighborGraph} interface.
	 */
	public static boolean isDirected(Graph g) {
		for (int i = 0; i < g.size(); i++) {
			for (int j : g.getNeighbours(i)) {
				if (!g.isEdge(j, i)) {
					return true;
				}
			}
		}
		return false;
	}

	// --------------------------------------------------------------------------

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

	// --------------------------------------------------------------------------

	public static int countTriads(LightweightStaticGraph g, int root) {
		if (g.directed() || !g.isSimple()) {
			throw new IllegalArgumentException();
		}
		// Assumes the graph is undirected and simple.
		int triads = 0;
		int degree = g.degree(root);

		// Counts how many (undirected) inter-neighbor edges there are.
		for (int i = 0; i < degree; i++) {
			for (int j = (i + 1); j < degree; j++) {
				int u = g.getNeighbor(root, i);
				int v = g.getNeighbor(root, j);
				if (g.isEdge(u, v)) {
					triads++;
				}
			}
		}

		return triads;
	}

	// --------------------------------------------------------------------------

	public static void dijkstra(IndexedNeighborGraph graph, int source,
			double[][] weights, final double[] minDists, int[] previous) {
		dijkstra(graph, NULL_FILTER, source, weights, minDists, previous);
	}

	/**
	 * Implementation of Dijkstra's algorithm using priority queues.
	 * 
	 * @param graph
	 * @param filter
	 * @param source
	 * @param weights
	 * @param minDists
	 * @param previous
	 */
	public static void dijkstra(IndexedNeighborGraph graph, IEdgeFilter filter,
			int source, double[][] weights, final double[] minDists,
			int[] previous) {

		Arrays.fill(previous, Integer.MAX_VALUE);
		Arrays.fill(minDists, Double.POSITIVE_INFINITY);

		minDists[source] = 0;
		PriorityQueue<Integer> vertexQueue = new PriorityQueue<Integer>(10,
				new Comparator<Integer>() {
					@Override
					public int compare(Integer o1, Integer o2) {
						return (int) Math.signum(minDists[o1] - minDists[o2]);
					}
				});

		vertexQueue.add(source);

		while (!vertexQueue.isEmpty()) {
			int u = vertexQueue.poll();
			// Visit each edge exiting u
			for (int i = 0; i < graph.degree(u); i++) {
				int v = graph.getNeighbor(u, i);
				if (filter.isForbidden(u, v)) {
					continue;
				}
				double weight = weights[u][v];
				double distanceThroughU = minDists[u] + weight;
				if (distanceThroughU < minDists[v]) {
					minDists[v] = distanceThroughU;
					previous[v] = u;

					// Increase key.
					vertexQueue.remove(v);
					vertexQueue.add(v);
				}
			}
		}
	}

	/**
	 * Computes the size of a path returned by a call do
	 * {@link #dijkstra(IndexedNeighborGraph, int, double[][], double[], int[])}
	 * .
	 * 
	 * @param previous
	 *            the "previous" vector used as output parameter in the call to
	 *            Dijkstra's algorithm.
	 * 
	 * @param target
	 *            the node for which the shortest path size is to be known.
	 * 
	 * @return the size of the path. If no path exists, returns -1.
	 */
	public static int dijkstraPathSize(int[] previous, int target) {
		return traverse(previous, target, null, -1);
	}

	/**
	 * Reconstructs the path from a "previous" vector returned by a call to
	 * {@link #dijkstra(IndexedNeighborGraph, int, double[][], double[], int[])}
	 * .
	 * 
	 * @param previous
	 *            the "previous" vector used as output parameter in the call to
	 *            Dijkstra's algorithm.
	 * 
	 * @param target
	 *            the node for which the shortest path size is to be known.
	 * 
	 * @param path
	 *            a vector long enough to accommodate the path.
	 * 
	 * @param lastPosition
	 *            let size(p) be the size of the path. This method will write it
	 *            in path[lastPosition - size(p) : lastPosition].
	 */
	public static void dijkstraPath(int[] previous, int target, int[] path,
			int lastPosition) {
		traverse(previous, target, path, lastPosition);
	}

	private static int traverse(int[] previous, int target, int[] storage,
			int offset) {
		int size = 0;
		int current = target;
		while (previous[current] != Integer.MAX_VALUE) {
			if (storage != null) {
				storage[offset - size] = current;
			}
			size++;
			current = previous[current];
		}
		
		if (size == 0) {
			return -1;
		} else {
			if (storage != null) {
				storage[offset - size] = current;
			}
			return size + 1;
		}
	}

	public static interface IEdgeFilter {
		public boolean isForbidden(int i, int j);
	}

	public static final IEdgeFilter NULL_FILTER = new IEdgeFilter() {
		@Override
		public boolean isForbidden(int i, int j) {
			return false;
		}
	};

}
