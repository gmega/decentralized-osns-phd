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
import peersim.core.Network;

@AutoConfig
public abstract class NeighborhoodExperiment implements ICDUnitExperiment {
	
	public static final String PAR_ID = "id";
	
	private final NodeRebootSupport fSupport;
	
	private final NeighborhoodLoader fLoader;
	
	private final Integer fRootId;
	
	protected final int fGraphProtocolId;
	
	private SNNode fRootNode;
	
	public NeighborhoodExperiment(
			@Attribute(Attribute.PREFIX) String prefix,
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
		
		int [] originals = fLoader.verticesOf(fRootId);
		IndexedNeighborGraph neighborhood = fLoader.neighborhood(fRootId);
		
		for (int i = 0; i < originals.length; i++) {
			SNNode node = (SNNode) Network.prototype.clone();
			node.setSNId(originals[i]);
			node.setID(i);
			Network.add(node);
			registry.registerNode(node);
			GraphProtocol gp = (GraphProtocol) node.getProtocol(fGraphProtocolId);
			gp.configure(node, neighborhood, registry);
		}
		
		fRootNode = (SNNode) Network.get(0);
		
		// Run node initializers.
		for (int i = 0; i < Network.size(); i++) {
			fSupport.initialize(Network.get(i));
		}
		
		chainInitialize();
	}
	
	public SNNode rootNode() {
		return fRootNode;
	}
	
	protected abstract void chainInitialize();
}
