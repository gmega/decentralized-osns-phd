package it.unitn.disi.churn.connectivity;

import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.simulator.core.Binding;
import it.unitn.disi.simulator.core.INetwork;
import it.unitn.disi.simulator.core.ISimulationEngine;

import java.util.Arrays;

/**
 * Extended single-source temporal connectivity experiment. Supports
 * measurements of receiver delay, and cloud nodes.
 * 
 * @author giuliano
 */
@Binding
public class ComplexTCE extends SimpleRDTCE {

	private ActivationSampler fSampler;

	private int[] fReachedFrom;

	private boolean[] fCloudNodes;

	public ComplexTCE(IndexedNeighborGraph graph, int source) {
		this(graph, source, null, null);
	}

	public ComplexTCE(IndexedNeighborGraph graph, int source, int[] cloudNodes,
			ActivationSampler sampler) {
		super(graph, source);

		fReachedFrom = new int[fGraph.size()];

		fSampler = sampler;
		fCloudNodes = new boolean[graph.size()];

		if (cloudNodes != null) {
			for (int i = 0; i < cloudNodes.length; i++) {
				fCloudNodes[cloudNodes[i]] = true;
			}
		}

		Arrays.fill(fReachedFrom, Integer.MAX_VALUE);
	}


	protected void reached(int source, int node, ISimulationEngine engine) {
		super.reached(source, node, engine);
		fReachedFrom[node] = source;
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

	public int reachedFrom(int i) {
		return fReachedFrom[i];
	}

	public ActivationSampler getActivationSampler() {
		return fSampler;
	}

}
