package it.unitn.disi.unitsim.experiments;

import it.unitn.disi.graph.GraphProtocol;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.unitsim.IGraphProvider;
import it.unitn.disi.unitsim.IUnitExperiment;
import it.unitn.disi.utils.peersim.INodeRegistry;
import it.unitn.disi.utils.peersim.NodeRebootSupport;
import it.unitn.disi.utils.peersim.NodeRegistry;
import it.unitn.disi.utils.peersim.SNNode;
import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.CommonState;
import peersim.core.Fallible;
import peersim.core.Network;

/**
 * A {@link GraphExperiment} is an experiment which is linked to a graph which
 * is a part of a larger set of graphs, possibly subgraphs of a huge graph.
 * 
 * The initial motivation here was to be able to run several experiments without
 * having to load the whole graph and/or without having to register the entire
 * graph as PeerSim nodes, since that would needlessly limit scalability.
 * 
 * @author giuliano
 */
@AutoConfig
public abstract class GraphExperiment implements IUnitExperiment {

	public static final String PAR_ID = "id";

	private final NodeRebootSupport fSupport;

	private final IGraphProvider fLoader;

	private final Integer fRootId;

	private long fStartingTime;

	protected final int fGraphProtocolId;

	private SNNode[] fNodes;

	private IndexedNeighborGraph fGraph;

	public GraphExperiment(@Attribute(Attribute.PREFIX) String prefix,
			@Attribute("id") Integer id,
			@Attribute("linkable") int graphProtocolId,
			@Attribute("NeighborhoodLoader") IGraphProvider loader) {
		fLoader = loader;
		fRootId = id;
		fGraphProtocolId = graphProtocolId;
		fSupport = new NodeRebootSupport(prefix);
		resetStartingTime();
	}

	@Override
	public Object getId() {
		return fRootId;
	}

	@Override
	public void initialize() {
		clearNetwork();
		INodeRegistry registry = NodeRegistry.getInstance();
		int[] originals = fLoader.verticesOf(fRootId);
		IndexedNeighborGraph graph = fLoader.subgraph(fRootId);
		SNNode nodes [] = new SNNode[graph.size()];
		for (int i = 0; i < originals.length; i++) {
			SNNode node = (SNNode) Network.prototype.clone();
			nodes[i] = node;
			node.setSNId(originals[i]);
			node.setID(i);
			Network.add(node);
			registry.registerNode(node);
			GraphProtocol gp = (GraphProtocol) node
					.getProtocol(fGraphProtocolId);
			gp.configure(node, graph, registry);
		}

		// Run node initializers.
		for (int i = 0; i < Network.size(); i++) {
			fSupport.initialize(Network.get(i));
		}

		initialize(graph, nodes);
	}

	public long startTime() {
		return fStartingTime;
	}

	void initialize(IndexedNeighborGraph neighborhood, SNNode [] nodes) {
		fGraph = neighborhood;
		fNodes = nodes;
		chainInitialize();
	}

	protected void resetStartingTime() {
		fStartingTime = CommonState.getTime();
	}

	protected long ellapsedTime() {
		return CommonState.getTime() - startTime();
	}
	
	protected IndexedNeighborGraph graph() {
		return fGraph;
	}

	protected int size() {
		return fNodes.length;
	}

	protected SNNode getNode(int id) {
		return fNodes[id];
	}

	protected void killAll() {
		for (int i = 0; i < size(); i++) {
			getNode(i).setFailState(Fallible.DEAD);
		}
	}


	protected abstract void chainInitialize();

	private void clearNetwork() {
		INodeRegistry registry = NodeRegistry.getInstance();
		registry.clear();
		while (Network.size() != 0) {
			Network.remove();
		}
	}
}
