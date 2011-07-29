package it.unitn.disi.newscasting.experiments;

import it.unitn.disi.graph.GraphProtocol;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.SubgraphDecorator;
import it.unitn.disi.newscasting.ComponentComputationService;
import it.unitn.disi.newscasting.internal.selectors.IUtilityFunction;
import it.unitn.disi.utils.IReference;
import it.unitn.disi.utils.peersim.ProtocolReference;

import java.util.ArrayList;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.Node;
import peersim.core.Protocol;
import peersim.graph.GraphAlgorithms;

@AutoConfig
public class ClusteringRanking implements IUtilityFunction<Node, Integer>,
		Protocol {

	/**
	 * Interval into which clustering coefficients in the [0,1] interval are
	 * mapped to.
	 */
	private static final int SCALING_RESOLUTION = Integer.MAX_VALUE;

	/**
	 * Static reference to the social graph (only works if it's static).
	 */
	private static IndexedNeighborGraph fGraph;

	/**
	 * {@link SubgraphDecorator} used to ease computation of the clustering
	 * coefficient.
	 */
	private static SubgraphDecorator fNeighborhood;

	private final IReference<ComponentComputationService> fComponents;

	private final IReference<GraphProtocol> fGraphProtocol;

	/**
	 * By default, assigns higher utility to higher clustering. Setting reverse
	 * to <code>true</code> reverses that.
	 */
	private final boolean fReverse;

	public ClusteringRanking(@Attribute("css") int cssId,
			@Attribute("linkable") int linkableId,
			@Attribute("reverse") boolean reverse) {
		this(new ProtocolReference<ComponentComputationService>(cssId),
				new ProtocolReference<GraphProtocol>(linkableId), reverse);
	}

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
		double clustering;
		if (subgraph.size() == 2) {
			clustering = 0.0;
		} else {
			clustering = GraphAlgorithms.clustering(subgraph,
					subgraph.map((int) base.getID()));
		}

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
		members.add((int) any.getID());
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
