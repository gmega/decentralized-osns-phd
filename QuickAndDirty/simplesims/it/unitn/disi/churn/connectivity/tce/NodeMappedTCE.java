package it.unitn.disi.churn.connectivity.tce;

import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.simulator.core.INetwork;
import it.unitn.disi.simulator.core.IProcess;

/**
 * {@link SimpleTCE} variant in which multiple nodes can be mapped into a single
 * underlying process.
 * 
 * @author giuliano
 */
public class NodeMappedTCE extends SimpleRDTCE {

	private static final long serialVersionUID = 1L;

	private final int[] fNodeMap;

	public NodeMappedTCE(IndexedNeighborGraph graph, int source, int[] nodeMap) {
		super(graph, source, false);
		fNodeMap = nodeMap;
	}

	@Override
	protected IProcess map(int node, INetwork network) {
		return network.process(fNodeMap[node]);
	}

}
