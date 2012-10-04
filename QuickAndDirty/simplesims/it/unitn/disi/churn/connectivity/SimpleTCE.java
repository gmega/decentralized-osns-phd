package it.unitn.disi.churn.connectivity;

import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.simulator.core.INetwork;
import it.unitn.disi.simulator.core.IEventObserver;
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

	private int fSource;

	private int fReachedCount;

	private double[] fReached;

	private boolean[] fDone;

	private BFSQueue fQueue;

	protected IndexedNeighborGraph fGraph;

	public SimpleTCE(IndexedNeighborGraph graph,
			int source) {
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

	private void recomputeReachabilities(RenewalProcess process,
			ISimulationEngine engine) {

		// Source being reached for the first time?
		if (!isReached(fSource)) {
			if (process.id() == fSource && process.isUp()) {
				sourceReached(process, engine);
			}
			// Source not reached yet, just return.
			else {
				return;
			}
		}

		for (int i = 0; i < fDone.length; i++) {
			// We start DFSs from all nodes that have
			// unvisited neighbors.
			if (!fDone[i] && isReached(i) && isUp(i, engine.network())) {
				fQueue.addLast(i);
				BFSExplore(engine);
			}
		}
	}

	protected void sourceReached(RenewalProcess process, ISimulationEngine engine) {
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

	protected void reached(int source, int node, ISimulationEngine engine) {
		fReached[node] = engine.clock().time();
		fReachedCount++;
	}

	protected boolean isUp(int node, INetwork network) {
		return network.process(node).isUp();
	}

	boolean isReached(int node) {
		return !Double.isNaN(fReached[node]);
	}

	@Override
	public boolean isDone() {
		return fReachedCount == fReached.length;
	}

	public double reachTime(int i) {
		return fReached[i] - fReached[fSource];
	}

}


