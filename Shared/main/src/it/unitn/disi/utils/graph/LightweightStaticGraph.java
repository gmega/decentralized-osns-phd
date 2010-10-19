package it.unitn.disi.utils.graph;

import it.unitn.disi.cli.GraphRemap;
import it.unitn.disi.codecs.ResettableGraphDecoder;
import it.unitn.disi.utils.collections.Pair;
import it.unitn.disi.utils.graph.BFSIterable.BFSIterator;
import it.unitn.disi.utils.logging.Progress;
import it.unitn.disi.utils.logging.ProgressTracker;

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
 * <BR>
 * This code has proved itself to be quite useful, and is in need of some
 * love.
 * TODO Refactor graph operations to the template pattern (GoF). 
 * 
 * @author giuliano
 */
public class LightweightStaticGraph implements IndexedNeighborGraph {
	
	// --------------------------------------------------------------------------
	// Methods for loading and doing simple, memory-efficient transforms on the
	// static graphs.
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
		// Three-phase loading process.
		System.err.println("Now loading.");
		
		// Phase 1 - compute memory requirements.
		System.err.println("Start pass 1 - computing required storage.");
		int maxId = -1;
		final Map<Integer, Integer> sizes = new HashMap<Integer, Integer>();
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
		
		// Phase 2 - allocates memory.
		System.err.println("Start pass 2 - allocating memory.");
		System.err.println("Maximum id is: " + maxId);
		int[][] adjacency = allocate(maxId + 1, new Accessor(){
			@Override
			public int get(int i) {
				return sizes.containsKey(i) ? sizes.get(i) : 0;
			}
		});

		// Frees up some memory...
		sizes.clear();

		// Phase 3 - goes again through the input, and loads the graph.
		System.err.println("Start pass 3 - loading graph into memory.");
		decoder.reset();
		int[] counters = new int[adjacency.length];
		while (decoder.hasNext()) {
			int source = decoder.getSource();
			int target = decoder.next();
			adjacency[source][counters[source]++] = target;
		}

		return new LightweightStaticGraph(adjacency, true);
	}
	
	public static LightweightStaticGraph undirect(LightweightStaticGraph source) {
		
		// 1 - Computes requirements for undirected version of the graph.
		final int [] sizes = new int[source.size()];
		for (int i = 0; i < source.size(); i++) {
			int [] neighbors = source.fastGetNeighbours(i);
			for(int j = 0; j < neighbors.length; j++) {
				int neighbor = neighbors[j];
				sizes[i]++;
				if (!source.isEdge(neighbor, i)) {
					sizes[neighbor]++;
				}
			}
		}

		// 2 - Allocates memory.
		int adjacency[][] = allocate(sizes.length, new Accessor() {
			@Override
			public int get(int i) {
				return sizes[i];
			}
		});
		
		// 3 - Generates undirected version.
		Arrays.fill(sizes, 0);
		for (int i = 0; i < source.size(); i++) {
			int [] neighbors = source.fastGetNeighbours(i);
			for(int j = 0; j < neighbors.length; j++) {
				int neighbor = neighbors[j];
				adjacency[i][sizes[i]++] = neighbor;
				if (!source.isEdge(neighbor, i)) {
					adjacency[neighbor][sizes[neighbor]++] = i;
				}
			}
		}
		
		return new LightweightStaticGraph(adjacency, false);
	}
	
	public static LightweightStaticGraph transitiveGraph(LightweightStaticGraph base, int order) {
		
		ProgressTracker tracker = Progress.newTracker("computing 2-hop neighborhood memory requirements", base.size());
		
		tracker.startTask();
		// 1 - Computes memory requirements.
		final int [] sizes = new int[base.size()];
		for (int i = 0; i < base.size(); i++) {
			BFSIterator it = new BFSIterator(base, i);
			// Skips the root.
			it.next();
			while (it.hasNext()) {
				// "a" is the node ID, "b" is the distance from the root.
				Pair<Integer, Integer> next = it.next();
				if (next.b > order) {
					break;
				}
				sizes[i]++;
			}
			tracker.tick();
		}
		tracker.done();
		
		// 2 - Allocates memory.
		int adjacency[][] = LightweightStaticGraph.allocate(sizes.length,
				new LightweightStaticGraph.Accessor() {
					@Override
					public int get(int i) {
						return sizes[i];
					}
				});
		
		tracker = Progress.newTracker("creating 2-hop neighbourhood", base.size());
		tracker.startTask();
		// 3 - Creates the graph.
		Arrays.fill(sizes, 0);
		for (int i = 0; i < base.size(); i++) {
			BFSIterator it = new BFSIterator(base, i);
			// Skips the root.
			it.next();
			while (it.hasNext()) {
				// "a" is the node ID, "b" is the distance from the root.
				Pair<Integer, Integer> next = it.next();
				if (next.b > order) {
					break;
				}
				adjacency[i][sizes[i]++] = next.a;
			}
			tracker.tick();
		}
		tracker.done();
		
		return new LightweightStaticGraph(adjacency, base.directed());

	}
	
	private static int[][] allocate(int size, Accessor accessor) {
		int [][] adjacency = new int[size][];
		int neighborless = 0;
		for (int i = 0; i < size; i++) {
			Integer neighbors = accessor.get(i);
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
		
		return adjacency;
	}
	
	// --------------------------------------------------------------------------
	// Support interfaces (internal). 
	// --------------------------------------------------------------------------
	
	public static interface Accessor {
		public int get(int i);
	}
	
	// --------------------------------------------------------------------------
	// Actual graph implementation.
	// --------------------------------------------------------------------------

	private final int[][] fAdjacency;
	
	private final boolean fDirected;

	private LightweightStaticGraph(int[][] adjacency, boolean directed) {
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
