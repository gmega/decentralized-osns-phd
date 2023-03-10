package it.unitn.disi.churn.connectivity.tce;

import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.simulator.core.Binding;
import it.unitn.disi.simulator.core.IClockData;
import it.unitn.disi.simulator.core.ISimulationEngine;
import it.unitn.disi.simulator.core.RenewalProcess;

import java.util.Arrays;

/**
 * {@link SimpleTCE} extension which adds tracking of receiver delay. The memory
 * costs are approximately three times higher as {@link SimpleTCE}.
 * 
 * @author giuliano
 */
@Binding
public class SimpleRDTCE extends BindingSimpleTCE {

	private static final long serialVersionUID = 1L;

	private double[] fUptimeSnapshot;

	private double[] fUptimeReached;

	public SimpleRDTCE(IndexedNeighborGraph graph, int source) {
		this(graph, source, true);
	}

	public SimpleRDTCE(IndexedNeighborGraph graph, int source, boolean binding) {
		super(graph, source, binding);

		fUptimeReached = new double[fGraph.size()];
		fUptimeSnapshot = new double[fGraph.size()];

		Arrays.fill(fUptimeSnapshot, 0);
		Arrays.fill(fUptimeReached, Double.MAX_VALUE);
	}

	@Override
	protected void reached(int source, int node, ISimulationEngine engine) {
		super.reached(source, node, engine);
		IClockData clock = engine.clock();
		fUptimeReached[node] = engine.network().process(node).uptime(clock);
	}

	@Override
	protected void sourceReached(RenewalProcess process,
			ISimulationEngine engine) {
		super.sourceReached(process, engine);
		snapshotUptimes(engine);
	}

	public double receiverDelay(int i) {
		return fUptimeReached[i] - fUptimeSnapshot[i];
	}

	private void snapshotUptimes(ISimulationEngine engine) {
		for (int i = 0; i < fUptimeSnapshot.length; i++) {
			fUptimeSnapshot[i] = engine.network().process(i)
					.uptime(engine.clock());
		}
	}
}
