package it.unitn.disi.unitsim.experiments;

import it.unitn.disi.graph.GraphProtocol;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.unitsim.ICDUnitExperiment;
import it.unitn.disi.unitsim.NeighborhoodLoader;
import it.unitn.disi.utils.peersim.INodeRegistry;
import it.unitn.disi.utils.peersim.NodeRebootSupport;
import it.unitn.disi.utils.peersim.NodeRegistry;
import it.unitn.disi.utils.peersim.SNNode;
import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.Linkable;
import peersim.core.Network;
import peersim.core.Node;
import peersim.graph.Graph;

/**
 * A {@link NeighborhoodExperiment} is an experiment which is linked to a
 * neighborhood in a graph. It makes the following assumptions:
 * <ol>
 * <li>that the neighborhood is uniquely identified by the ID of its root node;</li>
 * <li>that the experiment is constrained to a given neighborhood;</li>
 * <li>that the underlying graph might be loaded incrementally into memory.</li>
 * </ol>
 * 
 * The whole idea here is to be able to run several experiments without having
 * to load the whole graph and/or without having to register the entire graph as
 * PeerSim nodes, since that would needlessly limit scalability.
 * 
 * @author giuliano
 */
@AutoConfig
public abstract class NeighborhoodExperiment implements ICDUnitExperiment {

	public static final String PAR_ID = "id";

	private final NodeRebootSupport fSupport;

	private final NeighborhoodLoader fLoader;

	private final Integer fRootId;

	protected final int fGraphProtocolId;

	private SNNode fRootNode;
	
	private IndexedNeighborGraph fGraph;

	public NeighborhoodExperiment(@Attribute(Attribute.PREFIX) String prefix,
			@Attribute("id") Integer id,
			@Attribute("linkable") int graphProtocolId,
			@Attribute("NeighborhoodLoader") NeighborhoodLoader loader) {
		fLoader = loader;
		fRootId = id;
		fGraphProtocolId = graphProtocolId;
		fSupport = new NodeRebootSupport(prefix);
	}

	@Override
	public Object getId() {
		return fRootId;
	}

	@Override
	public void initialize() {
		INodeRegistry registry = NodeRegistry.getInstance();
		registry.clear();

		while (Network.size() != 0) {
			Network.remove();
		}

		int[] originals = fLoader.verticesOf(fRootId);
		IndexedNeighborGraph neighborhood = fLoader.neighborhood(fRootId);
		for (int i = 0; i < originals.length; i++) {
			SNNode node = (SNNode) Network.prototype.clone();
			node.setSNId(originals[i]);
			node.setID(i);
			Network.add(node);
			registry.registerNode(node);
			GraphProtocol gp = (GraphProtocol) node
					.getProtocol(fGraphProtocolId);
			gp.configure(node, neighborhood, registry);
		}

		// Run node initializers.
		for (int i = 0; i < Network.size(); i++) {
			fSupport.initialize(Network.get(i));
		}
		
		initialize(neighborhood, (SNNode) Network.get(0));
	}
	
	public void initialize(IndexedNeighborGraph neighborhood, SNNode rootNode) {
		fGraph = neighborhood;
		fRootNode = rootNode;
		chainInitialize();
	}

	public SNNode rootNode() {
		return fRootNode;
	}
	
	protected IndexedNeighborGraph graph() {
		return fGraph;
	}

	protected Linkable neighborhood() {
		return neighborhood(rootNode());
	}
	
	protected Linkable neighborhood(Node node) {
		return (Linkable) node.getProtocol(fGraphProtocolId);
	}

	protected abstract void chainInitialize();
}
