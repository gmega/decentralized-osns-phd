package it.unitn.disi.graph.large.catalog;

import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.SubgraphDecorator;
import peersim.graph.GraphAlgorithms;

public class NeighborhoodClustering extends NeighborhoodMetricComputer<Double> {

	@Override
	public Double compute(IndexedNeighborGraph source, int root) {
		SubgraphDecorator subgraph = neighborhood(source, root, true);
		return GraphAlgorithms.clustering(subgraph, subgraph.map(root));
	}

	@Override
	public String key() {
		return "clustering";
	}

	@Override
	public Class<Double> returnType() {
		return Double.class;
	}

}
