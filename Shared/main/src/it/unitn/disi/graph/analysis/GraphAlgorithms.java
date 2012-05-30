package it.unitn.disi.graph.analysis;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.stack.array.TIntArrayStack;
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
		return components(graph, new int[graph.size()]);
	}

	// --------------------------------------------------------------------------

	public static int[] components(Graph graph, int[] components) {
		DisjointSets sets = GraphAlgorithms.computeComponents(graph);
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

	// --------------------------------------------------------------------------

	/**
	 * Implementation of Dijkstra's algorithm using priority queues.
	 * 
	 * @param graph
	 *            the graph over which to compute the shortest paths.
	 * @param filter
	 *            an optional filter telling which edges are to be disregarded.
	 * @param source
	 *            the source vertex from which shortes paths are to be computed.
	 * @param weights
	 *            matrix containing the weights for the graph's edges.
	 * @param minDists
	 *            (output) array which will contain the minimum distance between
	 *            source and each destination.
	 * @param previous
	 *            (output) array containing the reverse tree of shortest paths
	 *            between each destination and the source.
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

	// --------------------------------------------------------------------------

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

	// --------------------------------------------------------------------------

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

	// --------------------------------------------------------------------------

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

	// --------------------------------------------------------------------------

	/**
	 * Performs a depth-first search on a graph starting from a given source.
	 * 
	 * @param graph
	 *            the graph on which to perform the DFS.
	 * @param source
	 *            source to start the search from.
	 * @param reachable
	 *            boolean array over which nodes that are reachable from the
	 *            source will be marked as <code>true</code>, and the others
	 *            will be marked as <code>false</code>.
	 * @param filter
	 *            an optional filter to exclude edges from the graph. If no
	 *            filtering is desided, use {@link #NULL_FILTER}.
	 */
	public static void dfs(IndexedNeighborGraph graph, int source,
			boolean[] reachable, IEdgeFilter filter) {
		Arrays.fill(reachable, false);
		reachable[source] = true;
		recDFS(graph, source, reachable, filter);
	}

	// --------------------------------------------------------------------------

	private static void recDFS(IndexedNeighborGraph graph, int source,
			boolean[] reachable, IEdgeFilter filter) {
		for (int i = 0; i < graph.degree(source); i++) {
			int neighbor = graph.getNeighbor(source, i);
			if (!reachable[neighbor] && !filter.isForbidden(source, neighbor)) {
				reachable[neighbor] = true;
				recDFS(graph, neighbor, reachable, filter);
			}
		}
	}

	// --------------------------------------------------------------------------

	public static class TarjanState {
		public int[] visit = new int[100];
		public int[] lowlink = new int[100];
		public int[] onstack = new int[100];

		public TIntArrayStack counter = new TIntArrayStack();
		public TIntArrayStack element = new TIntArrayStack();
		public TIntArrayStack current = new TIntArrayStack();

		public ArrayList<TIntArrayList> components = new ArrayList<TIntArrayList>();

		public int visitCounter = 0;

		public void ensureSize(int size) {
			visit = ensureSize(size, visit);
			lowlink = ensureSize(size, lowlink);
			onstack = ensureSize(size, onstack);
		}

		private int[] ensureSize(int size, int[] array) {
			if (array.length < size) {
				return Arrays.copyOf(array, size);
			}
			return array;
		}
	}

	// --------------------------------------------------------------------------

	public static int tarjan(TarjanState state, IndexedNeighborGraph graph) {
		// Initializes.
		initState(graph, state);

		int ncomps = 0;
		for (int i = 0; i < graph.size(); i++) {
			if (state.visit[i] == -1) {
				ncomps += tarjan(i, state, graph);
			}
		}

		return ncomps;
	}

	// --------------------------------------------------------------------------

	public static int tarjan(int starting, TarjanState s, IndexedNeighborGraph graph) {

		int upstream = 0;
		s.counter.push(0);
		s.element.push(starting);

		dfs: while (true) {

			int node = s.element.peek();

			if (s.visit[node] == -1) {
				s.lowlink[node] = s.visit[node] = s.visitCounter++;
				s.current.push(node);
				s.onstack[node] = 1;
			}

			for (int c = s.counter.peek(); c < graph.degree(node); c++) {
				int neighbor = graph.getNeighbor(node, c);
				if (s.visit[neighbor] == -1) {
					// Replace last element.
					s.counter.pop();
					s.counter.push(c + 1);

					s.element.push(neighbor);
					s.counter.push(0);
					continue dfs;
				} else if (s.onstack[neighbor] == 1
						&& (s.visit[neighbor] < s.visit[node])) {
					s.lowlink[node] = Math.min(s.lowlink[node],
							s.visit[neighbor]);
				}
			}

			// SCC root
			if (s.visit[node] == s.lowlink[node]) {
				TIntArrayList component = new TIntArrayList();
				
				upstream++;
				int popped;
				do {
					popped = s.current.pop();
					s.onstack[popped] = 0;
					component.add(popped);
				} while (popped != node);
				s.components.add(component);
			}

			int neighbor = s.element.pop();
			if (s.element.size() == 0) {
				break;
			}
			s.counter.pop();
			node = s.element.peek();
			s.lowlink[node] = Math.min(s.lowlink[node],
					s.lowlink[neighbor]);
		}

		return upstream;
	}

	// --------------------------------------------------------------------------

	private static void initState(IndexedNeighborGraph graph, TarjanState state) {

		checkArray(graph.size(), state.visit);
		checkArray(graph.size(), state.onstack);
		checkArray(graph.size(), state.lowlink);

		// Clears arrays.
		for (int i = 0; i < graph.size(); i++) {
			state.visit[i] = -1;
			state.onstack[i] = 0;
			state.lowlink[i] = i;
		}

		// Clears stacks.
		state.counter.clear();
		state.current.clear();
		state.element.clear();
		
		// Clear rest.
		state.visitCounter = 0;
		state.components.clear();
	}

	private static void checkArray(int size, int[] array) {
		if (array == null) {
			throw new NullPointerException();
		}

		if (array.length < size) {
			throw new IllegalArgumentException(
					"Arrays need to be as large as the number of vertices "
							+ "in the the graph.");
		}
	}

	/**
	 * An {@link IEdgeFilter} is a convenient way of excluding certain edges
	 * from consideration under the graph algorithms which support them.
	 * 
	 * @author giuliano
	 */
	public static interface IEdgeFilter {

		/**
		 * Tells whether or not an edge should be excluded from consideration.
		 * 
		 * @param i
		 *            the vertex from which the edge departs.
		 * @param j
		 *            the vertex in which the edge is incident.
		 * @return <code>true</code> if the edge should be considered, or
		 *         <code>false</code> otherwise.
		 */
		public boolean isForbidden(int i, int j);
	}

	// --------------------------------------------------------------------------

	public static final IEdgeFilter NULL_FILTER = new IEdgeFilter() {
		@Override
		public boolean isForbidden(int i, int j) {
			return false;
		}
	};

	// --------------------------------------------------------------------------
}
