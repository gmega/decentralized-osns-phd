package it.unitn.disi.churn.diffusion;

import java.util.Arrays;

import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.analysis.GraphAlgorithms;
import it.unitn.disi.graph.analysis.GraphAlgorithms.IEdgeFilter;
import it.unitn.disi.simulator.core.EDSimulationEngine;
import it.unitn.disi.simulator.core.INetwork;
import it.unitn.disi.simulator.core.ISimulationObserver;
import it.unitn.disi.simulator.core.Schedulable;
import it.unitn.disi.simulator.core.SimulationState;
import it.unitn.disi.simulator.measure.INodeMetric;

/**
 * {@link CoreTracker} maintains information about membership in the initially
 * connected core of nodes reachable from the source when it first logs in.
 * 
 * To work properly, {@link CoreTracker} requires that {@link DiffusionWick} be
 * installed before it. 
 * 
 * @author giuliano
 */
public class CoreTracker implements ISimulationObserver, INodeMetric<Boolean> {

	private boolean fInitial = true;

	private final HFlood fSource;

	private final int fPid;

	private int fCoreSize = -1;

	private boolean[] fConnectedCore;

	private boolean[] fCoreBuffer;

	public CoreTracker(HFlood source, int pid) {
		fSource = source;
		fPid = pid;

		fConnectedCore = new boolean[fSource.graph().size()];
		fCoreBuffer = new boolean[fSource.graph().size()];
	}

	@Override
	public void simulationStarted(EDSimulationEngine parent) {

	}

	@Override
	public void eventPerformed(SimulationState state, Schedulable schedulable) {
		INetwork network = state.network();

		// If source has already been reached, we are in maintenance mode.
		if (!fInitial) {
			recomputeCore(network);
			updateCore(network);
		}
		// Otherwise it's our first time seeing the source as reached.
		else if (fInitial && fSource.isReached()) {
			fInitial = false;
			fCoreSize = recomputeCore(network);
			System.arraycopy(fCoreBuffer, 0, fConnectedCore, 0,
					fCoreBuffer.length);
		}
	}

	/**
	 * Computes the set of nodes reachable from the source.
	 */
	private int recomputeCore(final INetwork network) {
		IndexedNeighborGraph graph = fSource.graph();
		Arrays.fill(fCoreBuffer, false);

		// Special case: if the source is down, means the connected core
		// has size zero.
		if (!network.process(fSource.id()).isUp()) {
			return 0;
		}

		return GraphAlgorithms.dfs(graph, fSource.id(), fCoreBuffer,
				new IEdgeFilter() {
					@Override
					public boolean isForbidden(int i, int j) {
						return !network.process(j).isUp();
					}
				});
	}

	private void updateCore(INetwork network) {
		for (int i = 0; i < fConnectedCore.length; i++) {
			// Node was reachable from the source, but is no longer.
			if (fConnectedCore[i] && !fCoreBuffer[i]) {
				HFlood protocol = (HFlood) network.process(i).getProtocol(fPid);
				// If node wasn't reached by the dissemination protocol, it
				// gets sawed off of the core.
				if (!protocol.isReached()) {
					fConnectedCore[i] = false;
					fCoreSize--;
					System.err.println("Core shrunk to " + fCoreSize);
				}
			}
		}
	}

	public boolean isPartOfConnectedCore(int id) {
		return fConnectedCore[id];
	}

	public int coreSize() {
		return fCoreSize;
	}

	@Override
	public boolean isDone() {
		return false;
	}

	@Override
	public boolean isBinding() {
		return false;
	}

	@Override
	public Object id() {
		return "coremembership";
	}

	@Override
	public Boolean getMetric(int i) {
		return fConnectedCore[i];
	}

}
