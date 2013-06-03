package it.unitn.disi.churn.diffusion;

import java.util.BitSet;

import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.analysis.GraphAlgorithms;
import it.unitn.disi.graph.analysis.GraphAlgorithms.IEdgeFilter;
import it.unitn.disi.simulator.core.INetwork;
import it.unitn.disi.simulator.core.IEventObserver;
import it.unitn.disi.simulator.core.Schedulable;
import it.unitn.disi.simulator.core.ISimulationEngine;
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
public class CoreTracker implements IEventObserver, INodeMetric<Boolean> {
	private static final long serialVersionUID = 1L;

	private boolean fInitial = true;

	private final DisseminationServiceImpl fSource;

	private final IndexedNeighborGraph fGraph;

	private final int fPid;

	private final int fSourceId;

	private int fCoreSize = -1;

	private BitSet fConnectedCore;

	private BitSet fCoreBuffer;

	public CoreTracker(DisseminationServiceImpl source,
			IndexedNeighborGraph graph, int sourceId, int pid) {
		fSource = source;
		fPid = pid;
		fSourceId = sourceId;
		fGraph = graph;

		fConnectedCore = new BitSet();
		fCoreBuffer = new BitSet();
	}

	@Override
	public void eventPerformed(ISimulationEngine state,
			Schedulable schedulable, double nextShift) {
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
			fConnectedCore.or(fCoreBuffer);
		}
	}

	/**
	 * Computes the set of nodes reachable from the source.
	 */
	private int recomputeCore(final INetwork network) {
		IndexedNeighborGraph graph = fGraph;
		fCoreBuffer.clear();

		// Special case: if the source is down, means the connected core
		// has size zero.
		if (!network.process(fSourceId).isUp()) {
			return 0;
		}

		return GraphAlgorithms.dfs(graph, fSourceId, fCoreBuffer,
				new IEdgeFilter() {
					@Override
					public boolean isForbidden(int i, int j) {
						return !network.process(j).isUp();
					}
				});
	}

	private void updateCore(INetwork network) {
		for (int i = fConnectedCore.nextSetBit(0); i >= 0; i = fConnectedCore
				.nextSetBit(i + 1)) {
			// Node was reachable from the source, but is no longer.
			if (fConnectedCore.get(i) && !fCoreBuffer.get(i)) {
				DisseminationServiceImpl protocol = (DisseminationServiceImpl) network
						.process(i).getProtocol(fPid);
				// If node wasn't reached by the dissemination protocol, it
				// gets sawed off of the core.
				if (!protocol.isReached()) {
					fConnectedCore.set(i, false);
					fCoreSize--;
				}
			}
		}
	}

	public boolean isPartOfConnectedCore(int id) {
		return fConnectedCore.get(id);
	}

	public int coreSize() {
		return fCoreSize;
	}

	@Override
	public boolean isDone() {
		return false;
	}

	@Override
	public Object id() {
		return "coremembership";
	}

	@Override
	public Boolean getMetric(int i) {
		return fConnectedCore.get(i);
	}

}
