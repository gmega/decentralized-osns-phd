package it.unitn.disi.newscasting.internal.selectors;

import java.util.ArrayList;

import it.unitn.disi.graph.GraphProtocol;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.SubgraphDecorator;
import it.unitn.disi.newscasting.ComponentComputationService;
import it.unitn.disi.utils.IReference;
import peersim.core.Node;
import peersim.graph.GraphAlgorithms;

public class ClusteringRanking implements IUtilityFunction<Node, Integer> {

	private static final int SCALING_RESOLUTION = Integer.MAX_VALUE;
	
	private static IndexedNeighborGraph fGraph;

	private static SubgraphDecorator fNeighborhood;

	private final IReference<ComponentComputationService> fComponents;

	private final IReference<GraphProtocol> fGraphProtocol;
	
	private final boolean fReverse;
	
	public ClusteringRanking(
			IReference<ComponentComputationService> components,
			IReference<GraphProtocol> graphProtocol, boolean reverse) {
		fComponents = components;
		fGraphProtocol = graphProtocol;
		fReverse = reverse;
	}

	@Override
	public int utility(Node base, Integer target) {
		SubgraphDecorator subgraph = subgraph(base, target);
		if (subgraph.size() == 1) {
			return SCALING_RESOLUTION;
		}
		
		double clustering = GraphAlgorithms.clustering(subgraph,
				(int) base.getID());
		if (fReverse) {
			clustering = 1.0 - clustering;
		}
		return (int) Math.round(clustering * SCALING_RESOLUTION);
	}

	private IndexedNeighborGraph graph(Node any) {
		if (fGraph == null) {
			GraphProtocol gp = fGraphProtocol.get(any);
			fGraph = gp.graph();
		}
		return fGraph;
	}

	private SubgraphDecorator subgraph(Node any, int componentId) {
		IndexedNeighborGraph graph = graph(any);
		if (fNeighborhood == null) {
			fNeighborhood = new SubgraphDecorator(graph, false);
		}
		ComponentComputationService css = fComponents.get(any);
		ArrayList<Integer> members = new ArrayList<Integer>();
		members.addAll(css.members(componentId));
		members.add((int)any.getID());
		fNeighborhood.setVertexList(members);
		return fNeighborhood;
	}

	@Override
	public boolean isDynamic() {
		return false;
	}

	public Object clone() {
		// Immutable.
		return this;
	}

}
