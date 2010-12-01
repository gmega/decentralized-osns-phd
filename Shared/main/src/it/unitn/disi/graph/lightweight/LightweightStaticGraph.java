package it.unitn.disi.graph.lightweight;

import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.cli.GraphRemap;
import it.unitn.disi.graph.codecs.ResettableGraphDecoder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import peersim.graph.Graph;

/**
 * Simple, fast, very lightweight, and very limited implementation of the
 * {@link Graph} interface, tailored specifically for read-only access of huge
 * (more than 200 million edges) static graphs.
 * <BR>

 * @author giuliano
 */
public class LightweightStaticGraph implements IndexedNeighborGraph {

	// --------------------------------------------------------------------------
	// Static creational and transform methods.
	// --------------------------------------------------------------------------
	
	/**
	 * Loads a graph into memory. Graphs must have a continuous range of IDs,
	 * meaning that vertices must be labeled from 0 to N, where N is the number
	 * of vertices in the graph.
	 * 
	 * {@link GraphRemap} can do the work of ID remapping for graphs which
	 * do not present continuous ID ranges.
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
	
	public static LightweightStaticGraph undirect(LightweightStaticGraph source) {
		LSGMakeUndirected undir = new LSGMakeUndirected();
		return undir.transform(source);
	}
	
	// --------------------------------------------------------------------------
	
	public static LightweightStaticGraph transitiveGraph(LightweightStaticGraph base, int order) {
		LSGCreateTransitive transitive = new LSGCreateTransitive(order);
		return transitive.transform(base);
	}

	// --------------------------------------------------------------------------
	// Actual graph implementation.
	// --------------------------------------------------------------------------

	private final int[][] fAdjacency;
	
	private final boolean fDirected;

	LightweightStaticGraph(int[][] adjacency, boolean directed) {
		fAdjacency = adjacency;
		fDirected = directed;
		sortAll();
	}

	public int degree(int i) {
		return fAdjacency[i].length;
	}

	public int size() {
		return fAdjacency.length;
	}

	public int[] fastGetNeighbours(int i) {
		return fAdjacency[i];
	}
	
	private void sortAll(){
		for (int i = 0; i < fAdjacency.length; i++) {
			Arrays.sort(fAdjacency[i]);
		}
	}

	public Collection<Integer> getNeighbours(int i) {
		ArrayList<Integer> neighbors = new ArrayList<Integer>(
				fAdjacency[i].length);

		for (int j = 0; j < fAdjacency[i].length; j++) {
			neighbors.add(j, fAdjacency[i][j]);
		}

		return neighbors;
	}
	
	public int getNeighbor(int node, int index) {
		return fAdjacency[node][index];
	}

	public boolean isEdge(int i, int j) {
		int index = Arrays.binarySearch(fAdjacency[i], j);
		if (index < 0 || index >= fAdjacency[i].length) {
			return false;
		}
		return fAdjacency[i][index] == j;
	}

	public boolean directed() {
		return fDirected;
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
