package it.unitn.disi.churn.connectivity;

import java.util.Arrays;

import it.unitn.disi.churn.BaseChurnSim;
import it.unitn.disi.churn.IChurnSim;
import it.unitn.disi.churn.RenewalProcess;
import it.unitn.disi.churn.RenewalProcess.State;
import it.unitn.disi.graph.IndexedNeighborGraph;

public class TemporalConnectivityExperiment implements IChurnSim {

	private BaseChurnSim fParent;

	private int fSource;

	private int fReachedCount;

	private double[] fReached;

	private boolean[] fDone;

	private IndexedNeighborGraph fGraph;

	public TemporalConnectivityExperiment(IndexedNeighborGraph graph, int source) {
		fGraph = graph;
		fSource = source;
		fReached = new double[fGraph.size()];
		fDone = new boolean[fGraph.size()];
		Arrays.fill(fReached, Double.NaN);
		Arrays.fill(fDone, false);
	}

	@Override
	public void simulationStarted(BaseChurnSim parent, Object stats) {
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
		if (!isReached(fSource) && process.id() == fSource && nw == State.up) {
			reached(fSource, time);
		}
		// Source not reached yet, just return.
		else {
			return;
		}

		for (int i = 0; i < fDone.length; i++) {
			if (!fDone[i] && isReached(i) && isUp(i)) {
				DFSExplore(i, time);
			}
		}
	}

	private void DFSExplore(int source, double time) {
		boolean done = true;
		for (int i = 0; i < fGraph.degree(source); i++) {
			int neighbor = fGraph.getNeighbor(source, i);
			if (!isReached(neighbor) && isUp(neighbor)) {
				reached(neighbor, time);
				DFSExplore(neighbor, time);
			}
			done &= isReached(neighbor);
		}

		fDone[source] = done;
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
		return fReached[i];
	}

	@Override
	public void printStats(Object stats) {
	}

}
