package it.unitn.disi.graph;

/**
 * An {@link IGraphVisitor} visits a graph vertex by vertex, and edge by edge.
 * The specifics on when edges and vertices will be visited is
 * implementation-specific. One of the key applications of {@link IGraphVisitor}
 * is for traversal of graphs that do not have an explicit representation (e.g.
 * they are algorithmically derived at every visit).
 * 
 * @author giuliano
 */
public interface IGraphVisitor {

	public static final int END_VISIT = -1;

	/**
	 * Visits a vertex. Vertices are guaranteed to be visited only once. When
	 * traversal is done, this method will be called with {@link #END_VISIT}.
	 * 
	 * @param i
	 *            the unique id of the vertex begin visited.
	 */
	public void visitVertex(int i);

	/**
	 * Visits an edge. Edges are guaranteed to be visited only once. Undirected
	 * edges may be visited either as (i, j) or (j, i), but never both.
	 * 
	 * @param i
	 *            the vertex from which the edge departs (not meaningful if the
	 *            edge is undirected).
	 * @param j
	 *            the vertex at which the edge incides (not meaningful if the
	 *            edge is undirected).
	 */
	public void visitEdge(int i, int j);

}
