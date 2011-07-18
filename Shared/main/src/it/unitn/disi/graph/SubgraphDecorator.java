package it.unitn.disi.graph;

import it.unitn.disi.utils.DenseIDMapper;
import it.unitn.disi.utils.IDMapper;
import it.unitn.disi.utils.AbstractIDMapper;
import it.unitn.disi.utils.SparseIDMapper;

import java.util.Collection;

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
public class SubgraphDecorator implements Graph, IDMapper {

	private final Graph fGraph;

	private AbstractIDMapper fIDMap;

	private boolean fChecked = false;

	private final Predicate<Integer> fPredicate = new Predicate<Integer>() {
		public boolean apply(Integer input) {
			return fIDMap.isMapped(input);
		}
	};

	private final Function<Integer, Integer> fMap = new Function<Integer, Integer>() {
		public Integer apply(Integer from) {
			return fIDMap.map(from);
		}
	};

	/**
	 * Creates a new subgraph decorator wrapping a given {@link Graph} g.
	 * 
	 * @param g
	 *            the graph to be wrapped.
	 */
	public SubgraphDecorator(Graph g, boolean unchecked) {
		this(g, unchecked, true);
	}
	
	public SubgraphDecorator(Graph g, boolean unchecked, boolean sparse) {
		fGraph = g;
		fChecked = !unchecked;
		fIDMap = sparse ? new SparseIDMapper() : new DenseIDMapper(g.size());
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
		fIDMap.clear();
		// The order doesn't really matter, as long as we give it one. If the
		// supplied collection is order-preserving, however, then our ordering
		// will also be.
		for (Integer element : list) {
			fIDMap.addMapping(element);
		}
	}

	// --------------------------------------------------------------------------

	public void setVertexList(int[] list) {
		fIDMap.clear();
		for (int element : list) {
			fIDMap.addMapping(element);
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
	public int map(int i) {
		return fIDMap.map(i);
	}

	// --------------------------------------------------------------------------

	/**
	 * Given the index of a vertex in the subgraph, returns its index in the
	 * original graph.
	 */
	public int reverseMap(int i) {
		return fIDMap.reverseMap(i);
	}

	// --------------------------------------------------------------------------

	public boolean isMapped(int id) {
		return fIDMap.isMapped(id);
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
		for (int neighbor : fGraph.getNeighbours(fIDMap.reverseMap(i))) {
			if (fIDMap.isMapped(neighbor)) {
				count++;
			}
		}

		return count;
	}

	// --------------------------------------------------------------------------

	private void checkIndex(int i) {
		if (i >= fIDMap.size() || i < 0) {
			throw new IllegalArgumentException("Vertex id " + i
					+ " is not valid.");
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

		return Collections2.transform(Collections2.filter(
				fGraph.getNeighbours(fIDMap.reverseMap(i)), fPredicate), fMap);
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
		return fGraph.getEdge(fIDMap.reverseMap(i), fIDMap.reverseMap(j));
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
		return fGraph.isEdge(fIDMap.reverseMap(i), fIDMap.reverseMap(j));
	}

	// --------------------------------------------------------------------------

	public Graph getGraph() {
		return fGraph;
	}

	// --------------------------------------------------------------------------

	public int size() {
		return fIDMap.size();
	}

	// --------------------------------------------------------------------------

	public Object getNode(int i) {
		if (fChecked) {
			checkIndex(i);
		}
		return fGraph.getNode(fIDMap.reverseMap(i));
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
