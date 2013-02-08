package it.unitn.disi.churn.connectivity.tce;

import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.simulator.core.INetwork;
import it.unitn.disi.simulator.core.IEventObserver;
import it.unitn.disi.simulator.core.IProcess;
import it.unitn.disi.simulator.core.RenewalProcess;
import it.unitn.disi.simulator.core.Schedulable;
import it.unitn.disi.simulator.core.ISimulationEngine;

import java.util.Arrays;

/**
 * Fast, single-source temporal connectivity experiment.
 * 
 * @author giuliano
 */
public class SimpleTCE implements IEventObserver {

	private static final long serialVersionUID = 1L;

	private int fSource;

	private int fReachedCount;

	private double[] fReached;

	private boolean[] fDone;

	private BFSQueue fQueue;

	protected IndexedNeighborGraph fGraph;

	public SimpleTCE(IndexedNeighborGraph graph, int source) {
		fGraph = graph;
		fSource = source;

		fReached = new double[fGraph.size()];
		fDone = new boolean[fGraph.size()];

		fQueue = new BFSQueue(fGraph.size());

		Arrays.fill(fReached, Double.NaN);
		Arrays.fill(fDone, false);
	}

	public int source() {
		return fSource;
	}

	@Override
	public void eventPerformed(ISimulationEngine engine,
			Schedulable schedulable, double nextShift) {
		RenewalProcess process = (RenewalProcess) schedulable;
		if (!process.isUp()) {
			return;
		}
		recomputeReachabilities(process, engine);
	}

	public boolean isReached(int node) {
		return !Double.isNaN(fReached[node]);
	}

	private void recomputeReachabilities(RenewalProcess process,
			ISimulationEngine engine) {

		INetwork network = engine.network();

		// Source being reached for the first time?
		if (!isReached(fSource)) {
			if (isSource(process, network) && process.isUp()) {
				sourceReached(process, engine);
			}
			// Source not reached yet, just return.
			else {
				return;
			}
		}

		for (int i = 0; i < fDone.length; i++) {
			// We start graph searches from all nodes that have
			// unvisited neighbors.
			if (!fDone[i] && isReached(i) && isUp(i, network)) {
				fQueue.addLast(i);
				BFSExplore(engine);
			}
		}
	}

	protected void sourceReached(RenewalProcess process,
			ISimulationEngine engine) {
		reached(fSource, fSource, engine);
	}

	private void BFSExplore(ISimulationEngine engine) {
		while (!fQueue.isEmpty()) {
			int current = fQueue.peekFirst();
			int degree = fGraph.degree(current);
			boolean done = true;

			for (int i = 0; i < degree; i++) {
				int neighbor = fGraph.getNeighbor(current, i);
				// Found unreached neighbor. If up, visits.
				if (!isReached(neighbor) && isUp(neighbor, engine.network())) {
					reached(current, neighbor, engine);
					fQueue.addLast(neighbor);
				}
				done &= isReached(neighbor);
			}

			fDone[current] = done;
			fQueue.removeFirst();
		}
	}

	public double endToEndDelay(int i) {
		return fReached[i] - fReached[fSource];
	}

	// -------------------------------------------------------------------------
	// Hook methods to override specific experiment behavior.
	// -------------------------------------------------------------------------

	@Override
	public boolean isDone() {
		return fReachedCount == fReached.length;
	}

	/**
	 * Called when a node is reached for the first time; i.e.
	 * {@link #isReached(int)} is <code>false</code> at the time this method is
	 * called.
	 * 
	 * @param source
	 *            the source from which this node is being reached.
	 * @param node
	 *            the node that's being reached (in the graph).
	 * @param engine
	 *            the current {@link ISimulationEngine}
	 */
	protected void reached(int source, int node, ISimulationEngine engine) {
		fReached[node] = engine.clock().time();
		fReachedCount++;
		
		if (isDone()) {
			done(engine);
		}
	}

	/**
	 * Tells whether some node is up or not.
	 * 
	 * @param node
	 *            id of the node to be tested.
	 * @param network
	 *            underlying {@link INetwork}.
	 * @return <code>true</code> if node is up, or false otherwise.
	 */
	protected boolean isUp(int node, INetwork network) {
		return map(node, network).isUp();
	}

	/**
	 * Tells whether the current process is the source or not.
	 * 
	 * @param process
	 * @param network
	 * @return
	 */
	protected boolean isSource(IProcess process, INetwork network) {
		return process.id() == map(fSource, network).id();
	}

	/**
	 * Allows ids to be flexibly mapped into processes. Mapping does not have to
	 * be one-to-one. The default implementation simply returns the process
	 * satisfying {@link IProcess#id()} == id.
	 * 
	 * @return an {@link IProcess} for the corresponding id.
	 */
	protected IProcess map(int id, INetwork network) {
		return network.process(id);
	}

	/**
	 * Called when the {@link SimpleTCE} switches into 'done' state (i.e. all
	 * nodes have been reached). This is guaranteed to be called only once.
	 */
	protected void done(ISimulationEngine engine) {
		// default impl. does nothing.
	}

}
