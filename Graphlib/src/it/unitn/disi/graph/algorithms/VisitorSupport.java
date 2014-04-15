package it.unitn.disi.graph.algorithms;

import it.unitn.disi.graph.IGraphVisitor;
import it.unitn.disi.graph.IndexedNeighborGraph;

/**
 * Simple support class implementing a straighforward (though not necessarily
 * efficient) visitor traversal strategy in {@link IndexedNeighborGraph}s.
 * 
 * @author giuliano
 */
public class VisitorSupport {

	private final IndexedNeighborGraph fGraph;

	private final boolean fDirected;

	public VisitorSupport(IndexedNeighborGraph graph, boolean directed) {
		fGraph = graph;
		fDirected = directed;
	}

	public void visit(IGraphVisitor visitor) {
		for (int i = 0; i < fGraph.size(); i++) {
			visitor.visitVertex(i);
			int degree = fGraph.degree(i);
			for (int j = 0; j < degree; j++) {
				int neighbor = fGraph.getNeighbor(i, j);

				// If the graph is undirected, we trim out the lower
				// triagle of the "adjacency matrix".
				if (fDirected || i < neighbor) {
					visitor.visitEdge(i, neighbor);
				}
			}
		}
	}

}
