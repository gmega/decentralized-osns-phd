package it.unitn.disi.churn.connectivity;

import java.util.Arrays;

import it.unitn.disi.churn.BaseChurnSim;
import it.unitn.disi.churn.IChurnSim;
import it.unitn.disi.churn.RenewalProcess;
import it.unitn.disi.churn.RenewalProcess.State;
import it.unitn.disi.graph.IndexedNeighborGraph;

/**
 * Fast, single-source temporal connectivity experiment.
 * 
 * @author giuliano
 */
public class TemporalConnectivityEstimator implements IChurnSim {

	private BaseChurnSim fParent;

	private int fSource;

	private int fReachedCount;

	private int[] fReachedFrom;

	private double[] fReached;

	private boolean[] fDone;

	private IndexedNeighborGraph fGraph;

	private BFSQueue fQueue;

	public TemporalConnectivityEstimator(IndexedNeighborGraph graph, int source) {
		fGraph = graph;
		fSource = source;
		fReachedFrom = new int[fGraph.size()];
		fReached = new double[fGraph.size()];
		fDone = new boolean[fGraph.size()];
		fQueue = new BFSQueue(fGraph.size());
		Arrays.fill(fReached, Double.NaN);
		Arrays.fill(fReachedFrom, Integer.MAX_VALUE);
		Arrays.fill(fDone, false);
	}

	@Override
	public void simulationStarted(BaseChurnSim parent) {
		fParent = parent;
	}

	@Override
	public void stateShifted(BaseChurnSim parent, double time,
			RenewalProcess process, State old, State nw) {

		if (nw != State.up) {
			return;
		}

		recomputeReachabilities(process, old, nw, time);
	}

	private void recomputeReachabilities(RenewalProcess process, State old,
			State nw, double time) {

		// Source being reached for the first time?
		if (!isReached(fSource)) {
			if (process.id() == fSource && nw == State.up) {
				reached(fSource, fSource, time);
			}
			// Source not reached yet, just return.
			else {
				return;
			}
		}

		for (int i = 0; i < fDone.length; i++) {
			// We start DFSs from all nodes that have
			// unvisited neighbors.
			if (!fDone[i] && isReached(i) && isUp(i)) {
				fQueue.addLast(i);
				BFSExplore(time);
			}
		}
	}

	private void BFSExplore(double time) {
		while (!fQueue.isEmpty()) {
			int current = fQueue.peekFirst();
			int degree = fGraph.degree(current);
			boolean done = true;

			for (int i = 0; i < degree; i++) {
				int neighbor = fGraph.getNeighbor(current, i);
				// Found unreached neighbor. If up, visits.
				if (!isReached(neighbor) && isUp(neighbor)) {
					reached(current, neighbor, time);
					fQueue.addLast(neighbor);
				}
				done &= isReached(neighbor);
			}

			fDone[current] = done;
			fQueue.removeFirst();
		}
	}

	private void reached(int source, int node, double time) {
		fReachedFrom[node] = source;
		fReached[node] = time;
		fReachedCount++;
	}

	private boolean isUp(int node) {
		return fParent.process(node).isUp();
	}

	private boolean isReached(int node) {
		return !Double.isNaN(fReached[node]);
	}

	@Override
	public boolean isDone() {
		return fReachedCount == fReached.length;
	}

	public double reachTime(int i) {
		return fReached[i] - fReached[fSource];
	}
	
	public int reachedFrom(int i) {
		return fReachedFrom[i];
	}
}

class BFSQueue {

	private int fRear = -1;

	private int fFront = -1;

	private int[] fQueue;

	public BFSQueue(int size) {
		ensure(size);
	}

	public void ensure(int size) {
		if (fQueue != null && size <= fQueue.length) {
			return;
		}

		fQueue = new int[size];
	}
	
	public int capacity() {
		return fQueue.length;
	}

	public boolean isEmpty() {
		return fFront == fRear;
	}

	public int peekFirst() {
		return fQueue[(fRear + 1) % fQueue.length];
	}

	public void addLast(int element) {
		fFront = (fFront + 1) % fQueue.length;
		fQueue[fFront] = element;
	}

	public int removeFirst() {
		fRear = (fRear + 1) % fQueue.length;
		return fQueue[fRear];
	}

}
