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

	private double[] fReached;

	private boolean[] fDone;

	private IndexedNeighborGraph fGraph;

	private DFSStack fStack;

	public TemporalConnectivityEstimator(IndexedNeighborGraph graph, int source) {
		fGraph = graph;
		fSource = source;
		fReached = new double[fGraph.size()];
		fDone = new boolean[fGraph.size()];
		fStack = new DFSStack(fGraph);
		Arrays.fill(fReached, Double.NaN);
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
				reached(fSource, time);
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
				DFSExplore(i, time);
			}
		}
	}

	private void DFSExplore(int source, double time) {
		DFSEntry current = fStack.push().bind(source);

		while (true) {
			while (current.hasNext()) {
				int neighbor = current.next();
				if (isReached(neighbor)) {
					continue;
				}
				// Found unreached neighbor. If up, visits.
				if (isUp(neighbor)) {
					reached(neighbor, time);
					current = fStack.push().bind(neighbor);
				}
				// If not up, means that we need to start a DFS from this node
				// in the future.
				else {
					current.markIncomplete();
				}
			}

			if (fStack.isEmpty()) {
				break;
			}
			
			fDone[current.node()] = current.explorationComplete();
			current = fStack.pop();
		}
	}

	private void reached(int node, double time) {
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
}

/**
 * Data structures for the DFS.
 * 
 * @author giuliano
 */

class DFSStack {

	private final IndexedNeighborGraph fGraph;

	private DFSEntry[] fEntries;

	private int fPointer = -1;

	public DFSStack(IndexedNeighborGraph graph) {
		fGraph = graph;
		ensure(graph.size());
	}

	public void ensure(int size) {
		if (fEntries != null && size <= fEntries.length) {
			return;
		}

		fEntries = new DFSEntry[size];
		for (int i = 0; i < fEntries.length; i++) {
			fEntries[i] = new DFSEntry(fGraph);
		}
	}

	public DFSEntry peek() {
		return fEntries[fPointer];
	}

	public DFSEntry push() {
		return fEntries[++fPointer];
	}

	public DFSEntry pop() {
		return fEntries[fPointer--];
	}

	public boolean isEmpty() {
		return fPointer == -1;
	}

}

class DFSEntry {

	private final IndexedNeighborGraph fGraph;

	private int fNode;

	private int fIndex;

	private int fDegree;

	private boolean fAllVisited;

	public DFSEntry(IndexedNeighborGraph graph) {
		fGraph = graph;
	}

	public DFSEntry bind(int node) {
		fNode = node;
		fDegree = fGraph.degree(node);
		fIndex = 0;
		fAllVisited = true;
		return this;
	}

	public int node() {
		return fNode;
	}

	public void markIncomplete() {
		fAllVisited = false;
	}

	public boolean explorationComplete() {
		return fAllVisited;
	}

	public boolean hasNext() {
		return fIndex < fDegree;
	}

	public int next() {
		return fGraph.getNeighbor(fNode, fIndex++);
	}
}
