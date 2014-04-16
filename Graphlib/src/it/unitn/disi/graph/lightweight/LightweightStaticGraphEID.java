package it.unitn.disi.graph.lightweight;

/**
 * {@link LightweightStaticGraphEID} is a variant of
 * {@link LightweightStaticGraph} that supports unique edge ids. Edges get
 * mapped into a continuous ID range from 0 to |E(G)|. It also supports the
 * construction of sparse matrices for associating data to edges efficiently.
 * 
 * @author giuliano
 * 
 */
public class LightweightStaticGraphEID extends LightweightStaticGraph {

	public static final LightweightStaticGraphEID asLightweightStaticGraphEID(
			LightweightStaticGraph original) {
		LightweightStaticGraphEID copy = new LightweightStaticGraphEID();

		original.initializeShallowCopy(copy);
		copy.initialize();

		return copy;
	}

	private static final long serialVersionUID = 1L;

	private volatile int[] fEdgeIndex;

	private volatile int[] fNeighborOffsets;

	protected LightweightStaticGraphEID() {
		// Clients should NOT EVER use this.
	}

	LightweightStaticGraphEID(int[][] adjacency) {
		super(adjacency);
		initialize();
	}

	/**
	 * Returns the id of an edge. If the graph is undirected,
	 * <code>edgeId(i,j)</code> will be the same as <code>edgeId(j,i)</code>. <BR>
	 * <BR>
	 * Ids are potentially midly expensive to retrieve, and should be cached by
	 * the client when requested from performance-critical code.
	 * 
	 * @param i
	 *            the first endpoint of the edge (i,j).
	 * @param j
	 *            the second endpoint of the edge (i,j).
	 * 
	 * @return the unique edge ID.
	 */
	public int edgeId(int i, int j) {
		if (!directed() && i > j) {
			int tmp = i;
			i = j;
			j = tmp;
		}

		return rawEdgeId(i, checkedIndex(indexOf(i, j)));
	}

	/**
	 * For all edges in directed graphs, and for some edges in undirected
	 * graphs, the id for an edge (i,j) can be retrieved more efficiently if the
	 * client already knows the index of j into i's neighbors, i.e. j is such
	 * that <code>j = graph.getNeighbor(i, index)</code>. This is often the
	 * case, for example, when the client is iterating through i's neighbors. <BR>
	 * <BR>
	 * In such cases, calling this method instead of {@link #edgeId(int, int)}
	 * can result in large performance gains.<BR>
	 * <BR>
	 * <b>NOTE:</b> The index parameter WILL NOT be verified to be valid by the
	 * method. If the user supplies an index which does not satisfy the
	 * contract, the behavior is unspecified.
	 * 
	 * 
	 * @param i
	 *            the first endpoint of the edge (i,j).
	 * @param j
	 *            the second endpoint of the edge (i,j).
	 * @param index
	 *            the index such that
	 *            <code>j = graph.getNeighbor(i, index)</code>.
	 * 
	 * 
	 * @return
	 */
	public int edgeId(int i, int j, int index) {
		/* Can't retrieve efficiently. :-( */
		if (!directed() && i > j) {
			index = indexOf(j, i);
			i = j;
		}

		return rawEdgeId(i, checkedIndex(index));
	}

	// -------------------------------------------------------------------------
	// The following are more low-level operations, which access the edge index
	// directly and may return garbage with higher probability if misused. ;-)
	// -------------------------------------------------------------------------

	/**
	 * Allows access to the underlying edge index. The edge index is
	 * one-dimensional array containing:
	 * <ol>
	 * <li>the degrees of each node, if the graph is directed</li>
	 * <li>the number of neighbors a node has in the upper triangle of the
	 * adjacency matrix, otherwise.</li>
	 * </ol>
	 * <BR>
	 * This structure is useful for implementing external sparse matrices that
	 * associate properties to edges, such as weight matrices, as
	 * one-dimensional arrays.
	 */
	public int edgeIndex(int i) {
		return fEdgeIndex[i];
	}

	/**
	 * Returns the "raw" edge id, which is the id that the k-th edge of node 'i'
	 * would be expected to have. The number returned is bogus if index >
	 * degree(i).
	 * 
	 * @param i
	 *            the reference node i.
	 * @param index
	 *            the k-th edge for which the id we wish to compute.
	 * 
	 * @return see method description.
	 */
	public int rawEdgeId(int i, int k) {
		return fEdgeIndex[i] - undirectedOffset(i) + k;
	}

	public int undirectedOffset(int i) {
		return directed() ? 0 : fNeighborOffsets[i];
	}

	private void initialize() {
		int[] index = new int[fAdjacency.length];
		int[] offsets = new int[fAdjacency.length];

		index[0] = 0;

		for (int i = 0; i < index.length; i++) {
			for (int j = 0; j < fAdjacency[i].length; j++) {
				int neighbor = fAdjacency[i][j];
				/*
				 * If we enter here, means the graph is undirected AND this edge
				 * is redundant (i.e. was already accounted for in a previous
				 * node). We therefore should skip it when computing the IDs.
				 */
				if (!directed() && i > neighbor) {
					offsets[i]++;
				}

				/* Otherwise the edge is being accounted for the first time. */
				else if (i < (index.length - 1)) {
					index[i + 1]++;
				}
			}

			if (i < (index.length - 1)) {
				index[i + 1] += index[i];
			}
		}

		fEdgeIndex = index;
		fNeighborOffsets = offsets;
	}

	private int checkedIndex(int index) {
		if (index == -1) {
			throw new IllegalArgumentException("Invalid neighbor specified.");
		}
		return index;
	}

}
