package it.unitn.disi.graph.large.catalog;

import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.SubgraphDecorator;

import java.util.ArrayList;

public abstract class NeighborhoodMetricComputer<T extends Number> implements
		ICatalogPart<T> {

	private SubgraphDecorator fSubgraph;

	protected SubgraphDecorator neighborhood(IndexedNeighborGraph source,
			int root, boolean includeRoot) {
		if (fSubgraph == null || fSubgraph.getGraph() != source) {
			fSubgraph = new SubgraphDecorator(source, true);
		}

		ArrayList<Integer> members = new ArrayList<Integer>();
		members.addAll(source.getNeighbours(root));
		if (includeRoot) {
			members.add(root);	
		}
		fSubgraph.setVertexList(members);
		return fSubgraph;
	}

}
