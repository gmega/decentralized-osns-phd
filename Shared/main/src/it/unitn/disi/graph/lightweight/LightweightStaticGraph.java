package it.unitn.disi.graph.lightweight;

import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.analysis.GraphAlgorithms;
import it.unitn.disi.graph.cli.GraphRemap;
import it.unitn.disi.graph.codecs.ResettableGraphDecoder;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import peersim.graph.Graph;

/**
 * Simple, fast, very lightweight, and very limited implementation of the
 * {@link Graph} interface, tailored specifically for read-only access of huge
 * (more than 200 million edges) static graphs. <BR>
 * 
 * @author giuliano
 */
public class LightweightStaticGraph implements IndexedNeighborGraph,
		Serializable {

	/**
	 * Version UID.
	 */
	private static final long serialVersionUID = -2004042032852047149L;

	// --------------------------------------------------------------------------
	// Static creational and transform methods.
	// --------------------------------------------------------------------------

	/**
	 * Loads a graph into memory. Graphs must have a continuous range of IDs,
	 * meaning that vertices must be labeled from 0 to N, where N is the number
	 * of vertices in the graph.
	 * 
	 * {@link GraphRemap} can do the work of ID remapping for graphs which do
	 * not present continuous ID ranges.
	 * 
	 * @param decoder
	 *            a {@link ResettableGraphDecoder} pointing to the graph to be
	 *            loaded.
	 * 
	 * @return a {@link LightweightStaticGraph} containing the loaded graph.
	 * 
	 * @throws IOException
	 *             if one is thrown by the {@link ResettableGraphDecoder}.
	 */
	public static LightweightStaticGraph load(ResettableGraphDecoder decoder)
			throws IOException {
		LSGCreator creator = new LSGLoader(decoder);
		return creator.create();
	}

	// --------------------------------------------------------------------------

	public static LightweightStaticGraph fromAdjacency(int[][] adjlists) {
		return new LightweightStaticGraph(adjlists);
	}

	// --------------------------------------------------------------------------

	public static LightweightStaticGraph fromGraph(Graph g) {
		int[][] a = new int[g.size()][];
		for (int i = 0; i < a.length; i++) {
			a[i] = new int[g.degree(i)];
			int j = 0;
			for (int neighbor : g.getNeighbours(i)) {
				a[i][j] = neighbor;
				j++;
			}
		}
		return fromAdjacency(a);
	}

	// --------------------------------------------------------------------------

	public static LightweightStaticGraph subgraph(
			LightweightStaticGraph source, int...vertices) {
		LSGCreateSubgraph subgraph = new LSGCreateSubgraph(vertices);
		return subgraph.transform(source);
	}

	// --------------------------------------------------------------------------

	public static LightweightStaticGraph undirect(LightweightStaticGraph source) {
		LSGMakeUndirected undir = new LSGMakeUndirected();
		return undir.transform(source);
	}

	// --------------------------------------------------------------------------

	public static LightweightStaticGraph transitiveGraph(
			LightweightStaticGraph base, int order) {
		LSGCreateTransitive transitive = new LSGCreateTransitive(order);
		return transitive.transform(base);
	}

	// --------------------------------------------------------------------------
	// Actual graph implementation.
	// --------------------------------------------------------------------------

	private final int[][] fAdjacency;

	private final int fEdges;

	private transient Boolean fDirected;

	private transient Boolean fSimple;

	private transient Boolean fConnected;

	LightweightStaticGraph(int[][] adjacency) {
		fAdjacency = adjacency;
		fEdges = countEdges(adjacency);
		sortAll();
	}

	private int countEdges(int[][] adjacency) {
		int edges = 0;
		for (int i = 0; i < adjacency.length; i++) {
			edges += adjacency[i].length;
		}
		return edges;
	}

	private void sortAll() {
		for (int i = 0; i < fAdjacency.length; i++) {
			Arrays.sort(fAdjacency[i]);
		}
	}

	// --------------------------------------------------------------------------
	// IndexedNeighborhoodGraph interface.
	// --------------------------------------------------------------------------

	/**
	 * {@link Graph#isEdge(int, int)} implementation which works by doing a
	 * binary search over an array of neighbors sorted by id. This is O(log(n))
	 * on the degree, but allows us to obtain significant memory savings.
	 */
	public boolean isEdge(int i, int j) {
		return indexOf(i, j) != -1;
	}

	public int indexOf(int i, int j) {
		int index = Arrays.binarySearch(fAdjacency[i], j);
		if (index < 0 || index >= fAdjacency[i].length) {
			return -1;
		}
		return fAdjacency[i][index] == j ? index : -1;
	}

	/**
	 * @return the number of vertices in the graph.
	 */
	public int size() {
		return fAdjacency.length;
	}

	/**
	 * @return the <b>out degree</b> of a vertex.
	 */
	public int degree(int i) {
		return fAdjacency[i].length;
	}

	public Collection<Integer> getNeighbours(int i) {
		ArrayList<Integer> neighbors = new ArrayList<Integer>(
				fAdjacency[i].length);

		for (int j = 0; j < fAdjacency[i].length; j++) {
			neighbors.add(j, fAdjacency[i][j]);
		}

		return neighbors;
	}

	/**
	 * Same as {@link #getNeighbours(int)}, except that it actually returns the
	 * <b>internal</b> array being used to store data instead of copying it to a
	 * {@link Collection}. This is highly efficient, but writing to this array
	 * will result in unspecified behavior.
	 */
	public int[] fastGetNeighbours(int i) {
		return fAdjacency[i];
	}

	/**
	 * Returns <code>true</code> if this graph is directed, or
	 * <code>false</code> otherwise.
	 */
	public boolean directed() {
		if (fDirected == null) {
			fDirected = GraphAlgorithms.isDirected(this);
		}
		return fDirected;
	}

	/**
	 * Returns <code>true</code> if this graph is simple, or <code>false</code>
	 * otherwise.
	 */
	public boolean isSimple() {
		if (fSimple == null) {
			fSimple = GraphAlgorithms.isSimple(this);
		}
		return fSimple;
	}

	/**
	 * Returns <code>true</code> if this graph is connected, or
	 * <code>false</code> otherwise.
	 */
	public boolean isConnected() {
		if (fConnected == null) {
			fConnected = GraphAlgorithms.isConnected(this);
		}

		return fConnected;
	}

	/**
	 * Returns the number of edges in this graph.
	 */
	public int edgeCount() {
		return directed() ? fEdges : (int) (fEdges / 2.0);
	}

	public int getNeighbor(int node, int index) {
		return fAdjacency[node][index];
	}

	public Object getNode(int i) {
		return null;
	}

	public boolean clearEdge(int i, int j) {
		return false;
	}

	public boolean setEdge(int i, int j) {
		throw new UnsupportedOperationException();
	}

	public Object getEdge(int i, int j) {
		return null;
	}
}
