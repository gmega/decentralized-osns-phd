package it.unitn.disi.cli;

import it.unitn.disi.codecs.ResettableGraphDecoder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import peersim.graph.Graph;

/**
 * Simple, fast, very lightweight, and very limited implementation of the
 * {@link Graph} interface, tailored specifically for read-only access of huge
 * (more than 200 million edges) static graphs.
 * 
 * @author giuliano
 */
public class LightweightStaticGraph implements Graph {

	/**
	 * Loads a graph into memory. Graphs must have a continuous range of IDs,
	 * meaning that vertices must be labeled from 0 to N, where N is the number
	 * of vertices in the graph.
	 * 
	 * {@link ByteGraphRemap} can do the work of ID remapping for graphs which
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
		// Three-phase loading process.
		System.err.println("Now loading.");
		// Phase 1 - compute memory requirements and allocate memory.
		System.err.println("Start pass 1 - computing required storage.");
		int maxId = -1;
		Map<Integer, Integer> sizes = new HashMap<Integer, Integer>();
		while (decoder.hasNext()) {
			int source = decoder.getSource();
			int target = decoder.next();
			Integer sourceCount = sizes.get(source);
			if (sourceCount == null) {
				sourceCount = 0;
			}
			sizes.put(source, sourceCount + 1);
			maxId = Math.max(maxId, source);
			maxId = Math.max(maxId, target);
		}

		
		System.err.println("Start pass 2 - allocating memory.");
		System.err.println("Maximum id is: " + maxId);
		int neighborless = 0;
		int[][] adjacency = new int[maxId + 1][];
		for (int i = 0; i <= maxId; i++) {
			Integer neighbors = sizes.get(i);
			if (neighbors == null) {
				neighbors = 0;
			}
			
			if (neighbors == 0) {
				neighborless++;
			}
			
			adjacency[i] = new int[neighbors];
		}
		
		if (neighborless > 0) {
			System.err.println("Warning: there were (" + neighborless
					+ ") nodes without neighbors (ID holes?).");
		}

		// Frees up some memory...
		sizes = null;

		// Phase 2 - loads the graph.
		System.err.println("Start pass 3 - loading graph into memory.");
		decoder.reset();
		int[] counters = new int[adjacency.length];
		while (decoder.hasNext()) {
			int source = decoder.getSource();
			int target = decoder.next();
			adjacency[source][counters[source]++] = target;
		}

		return new LightweightStaticGraph(adjacency);
	}

	private final int[][] fAdjacency;

	private LightweightStaticGraph(int[][] adjacency) {
		fAdjacency = adjacency;
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
	
	public void sortAll(){
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

	public boolean isEdge(int i, int j) {
		for (int k = 0; k < fAdjacency[i].length; k++) {
			if (fAdjacency[i][k] == j) {
				return true;
			}
		}

		return false;
	}

	public boolean directed() {
		return false;
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
