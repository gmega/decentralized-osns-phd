package it.unitn.disi.churn.connectivity;

import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.simulator.core.Binding;
import it.unitn.disi.simulator.core.IClockData;
import it.unitn.disi.simulator.core.INetwork;
import it.unitn.disi.simulator.core.RenewalProcess;
import it.unitn.disi.simulator.core.ISimulationEngine;

import java.util.Arrays;

/**
 * Extended single-source temporal connectivity experiment. Supports
 * measurements of receiver delay, and cloud nodes.
 * 
 * @author giuliano
 */
@Binding
public class ComplexTCE extends SimpleTCE {

	private ActivationSampler fSampler;

	private int[] fReachedFrom;

	private double[] fUptimeSnapshot;

	private double[] fUptimeReached;

	private boolean[] fCloudNodes;

	public ComplexTCE(IndexedNeighborGraph graph, int source) {
		this(graph, source, null, null);
	}

	public ComplexTCE(IndexedNeighborGraph graph, int source, int[] cloudNodes,
			ActivationSampler sampler) {
		super(graph, source);

		fReachedFrom = new int[fGraph.size()];
		fUptimeReached = new double[fGraph.size()];
		fUptimeSnapshot = new double[fGraph.size()];

		fSampler = sampler;
		fCloudNodes = new boolean[graph.size()];

		if (cloudNodes != null) {
			for (int i = 0; i < cloudNodes.length; i++) {
				fCloudNodes[cloudNodes[i]] = true;
			}
		}

		Arrays.fill(fUptimeSnapshot, 0);
		Arrays.fill(fUptimeReached, Double.MAX_VALUE);
		Arrays.fill(fReachedFrom, Integer.MAX_VALUE);
	}

	@Override
	protected void sourceReached(RenewalProcess process,
			ISimulationEngine engine) {
		super.sourceReached(process, engine);
		snapshotUptimes(engine);
	}

	protected void reached(int source, int node, ISimulationEngine engine) {
		super.reached(source, node, engine);
		IClockData clock = engine.clock();
		fReachedFrom[node] = source;
		fUptimeReached[node] = engine.network().process(node).uptime(clock);
		if (fSampler != null) {
			fSampler.reached(node, this);
		}

		if (isDone()) {
			engine.unbound(this);
		}
	}

	private void snapshotUptimes(ISimulationEngine engine) {
		for (int i = 0; i < fUptimeSnapshot.length; i++) {
			fUptimeSnapshot[i] = engine.network().process(i)
					.uptime(engine.clock());
		}
	}

	protected boolean isUp(int node, INetwork network) {
		return fCloudNodes[node] || super.isUp(node, network);
	}

	public double perceivedDelay(int i) {
		return fUptimeReached[i] - fUptimeSnapshot[i];
	}

	public int reachedFrom(int i) {
		return fReachedFrom[i];
	}

	public ActivationSampler getActivationSampler() {
		return fSampler;
	}

}
