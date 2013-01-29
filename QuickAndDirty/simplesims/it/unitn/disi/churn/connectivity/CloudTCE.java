package it.unitn.disi.churn.connectivity;

import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.simulator.core.Binding;
import it.unitn.disi.simulator.core.INetwork;
import it.unitn.disi.simulator.core.ISimulationEngine;

/**
 * Extended single-source temporal connectivity experiment. Supports cloud
 * nodes.
 * 
 * @author giuliano
 */
@Binding
public class CloudTCE extends SimpleRDTCE {

	private static final long serialVersionUID = 1L;

	private ActivationSampler fSampler;

	private boolean[] fCloudNodes;

	public CloudTCE(IndexedNeighborGraph graph, int source) {
		this(graph, source, null, null);
	}

	public CloudTCE(IndexedNeighborGraph graph, int source, int[] cloudNodes,
			ActivationSampler sampler) {
		super(graph, source);

		fSampler = sampler;
		fCloudNodes = new boolean[graph.size()];

		if (cloudNodes != null) {
			for (int i = 0; i < cloudNodes.length; i++) {
				fCloudNodes[cloudNodes[i]] = true;
			}
		}
	}

	protected void reached(int source, int node, ISimulationEngine engine) {
		super.reached(source, node, engine);
		if (fSampler != null) {
			fSampler.reached(node, this);
		}

		if (isDone()) {
			engine.unbound(this);
		}
	}

	protected boolean isUp(int node, INetwork network) {
		return fCloudNodes[node] || super.isUp(node, network);
	}

	public ActivationSampler getActivationSampler() {
		return fSampler;
	}

}
