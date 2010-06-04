package it.unitn.disi.utils.graph;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import peersim.graph.Graph;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

/**
 * Read-only, memory-efficient wrapper for a {@link Graph} interface which
 * allows some operations to be ran on a subgraph of the original graph.
 * 
 * @author giuliano
 * 
 */
public class SubgraphDecorator implements Graph {

	private final Graph fGraph;

	private int [] fMappings = new int[0];

	private Map<Integer, Integer> fInvMap = new HashMap<Integer, Integer>();
	
	private boolean fChecked = false;

	private final Predicate<Integer> fPredicate = new Predicate<Integer>() {
		public boolean apply(Integer input) {
			return fInvMap.containsKey(input);
		}
	};

	private final Function<Integer, Integer> fMap = new Function<Integer, Integer>() {
		public Integer apply(Integer from) {
			return fInvMap.get(from);
		}
	};

	/**
	 * Creates a new subgraph decorator wrapping a given {@link Graph} g.
	 * 
	 * @param g
	 *            the graph to be wrapped.
	 */
	public SubgraphDecorator(Graph g, boolean unchecked) {
		fGraph = g;
		fChecked = !unchecked;
	}

	// --------------------------------------------------------------------------

	/**
	 * Sets the vertex list which will define the subgraph.
	 * 
	 * @param list
	 *            the list of vertices defining the subgraph. Note that all
	 *            vertices in the original graph will be valid in the subgraph.
	 */
	public void setVertexList(Collection<Integer> list) {
		int size = list.size();
		
		fInvMap.clear();
		ensureMappingsSize(size);
		
		// The order doesn't really matter, as long as we give it one.
		Iterator<Integer> it = list.iterator();
		for (int i = 0; i < size; i++) {
			int idx = it.next();
			fInvMap.put(idx, i);
			fMappings[i] = idx;
		}
	}

	// --------------------------------------------------------------------------

	/**
	 * Given the index of a vertex in the original graph, returns its index in
	 * the subgraph.
	 * 
	 * @param i
	 *            the index of a vertex in the original graph.
	 * 
	 * @return the id of the vertex in the subgraph.
	 * 
	 * @throws IllegalArgumentException
	 *             if the vertex with id <b>i</b> doesn't belong to the
	 *             subgraph.
	 */
	public int idOf(int i) {
		if (fChecked) {
			checkInverseIndex(i);
		}
		
		return fInvMap.get(i);
	}
	

	// --------------------------------------------------------------------------
	public int inverseIdOf(int i) {
		return fMappings[i];
	}

	// --------------------------------------------------------------------------

	/**
	 * Returns the degree of a vertex in the subgraph.
	 * 
	 * @throws IllegalArgumentException
	 *             if the vertex is not part of the subgraph.
	 */
	public int degree(int i) {
		if (fChecked) {
			checkIndex(i);
		}

		int count = 0;
		for (int neighbor : fGraph.getNeighbours(fMappings[i])) {
			if (fInvMap.containsKey(neighbor)) {
				count++;
			}
		}

		return count;
	}
	
	// --------------------------------------------------------------------------
	
	private void ensureMappingsSize(int value) {
		if (fMappings.length < value) {
			fMappings = new int[value * 2];
		}
	}

	// --------------------------------------------------------------------------

	private void checkIndex(int i) {
		if (i >= fInvMap.size() || i < 0) {
			throw new IllegalArgumentException("Vertex id " + i
					+ " is not valid.");
		}
	}

	// --------------------------------------------------------------------------

	private void checkInverseIndex(int i) {
		if (!fInvMap.containsKey(i)) {
			throw new IllegalArgumentException("Vertex " + i
					+ " is not a part of the subraph.");
		}
	}

	// --------------------------------------------------------------------------

	/**
	 * Returns the neighbors of a vertex in the subgraph.
	 * 
	 * @param i
	 *            the vertex for which the neighbor collection is to be
	 *            returned.
	 */
	public Collection<Integer> getNeighbours(int i) {
		if (fChecked) {
			checkIndex(i);
		}
		
		return Collections2.transform(Collections2.filter(fGraph
				.getNeighbours(fMappings[i]), fPredicate), fMap);
	}

	// --------------------------------------------------------------------------

	/**
	 * Same as {@link Graph#getEdge(int, int)}.
	 */
	public Object getEdge(int i, int j) {
		if (fChecked) {
			checkIndex(i);
			checkIndex(j);
		}
		return fGraph.getEdge(fMappings[i], fMappings[j]);
	}

	// --------------------------------------------------------------------------

	/**
	 * Same as {@link Graph#directed()}.
	 */
	public boolean directed() {
		return fGraph.directed();
	}

	// --------------------------------------------------------------------------

	public boolean isEdge(int i, int j) {
		if (fChecked) {
			checkIndex(i);
			checkIndex(j);
		}
		return fGraph.isEdge(fMappings[i], fMappings[j]);
	}

	// --------------------------------------------------------------------------

	public Graph getGraph() {
		return fGraph;
	}

	// --------------------------------------------------------------------------

	public int size() {
		return fInvMap.size();
	}

	// --------------------------------------------------------------------------
	
	public Object getNode(int i) {
		if (fChecked) {
			checkIndex(i);
		}
		return fGraph.getNode(fMappings[i]);
	}

	// --------------------------------------------------------------------------

	public boolean clearEdge(int i, int j) {
		throw new UnsupportedOperationException();
	}

	// --------------------------------------------------------------------------

	public boolean setEdge(int i, int j) {
		throw new UnsupportedOperationException();
	}
}
