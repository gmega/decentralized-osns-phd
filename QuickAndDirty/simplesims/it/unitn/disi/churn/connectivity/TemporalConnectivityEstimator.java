package it.unitn.disi.churn.connectivity;

import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.simulator.core.EDSimulationEngine;
import it.unitn.disi.simulator.core.INetwork;
import it.unitn.disi.simulator.core.ISimulationObserver;
import it.unitn.disi.simulator.core.RenewalProcess;
import it.unitn.disi.simulator.core.Schedulable;

import java.util.Arrays;

/**
 * Fast, single-source temporal connectivity experiment. Supports special
 * "cloud" nodes that are treated as always up.
 * 
 * @author giuliano
 */
public class TemporalConnectivityEstimator implements ISimulationObserver {

	private EDSimulationEngine fParent;

	private ActivationSampler fSampler;

	private int fSource;

	private int fReachedCount;

	private int[] fReachedFrom;

	private double[] fUptimeSnapshot;

	private double[] fUptimeReached;

	private double[] fReached;

	private boolean[] fDone;

	private boolean[] fCloudNodes;

	private IndexedNeighborGraph fGraph;

	private BFSQueue fQueue;

	public TemporalConnectivityEstimator(IndexedNeighborGraph graph, int source) {
		this(graph, source, null, null);
	}

	public TemporalConnectivityEstimator(IndexedNeighborGraph graph,
			int source, int[] cloudNodes, ActivationSampler sampler) {
		fGraph = graph;
		fSource = source;

		fReachedFrom = new int[fGraph.size()];
		fReached = new double[fGraph.size()];
		fUptimeReached = new double[fGraph.size()];
		fUptimeSnapshot = new double[fGraph.size()];
		fDone = new boolean[fGraph.size()];

		fQueue = new BFSQueue(fGraph.size());
		fSampler = sampler;
		fCloudNodes = new boolean[graph.size()];

		if (cloudNodes != null) {
			for (int i = 0; i < cloudNodes.length; i++) {
				fCloudNodes[cloudNodes[i]] = true;
			}
		}

		Arrays.fill(fReached, Double.NaN);
		Arrays.fill(fUptimeSnapshot, 0);
		Arrays.fill(fUptimeReached, Double.MAX_VALUE);
		Arrays.fill(fReachedFrom, Integer.MAX_VALUE);
		Arrays.fill(fDone, false);
	}
	
	public int source() {
		return fSource;
	}

	@Override
	public void simulationStarted(EDSimulationEngine parent) {
		fParent = parent;
	}

	@Override
	public void eventPerformed(INetwork parent, double time,
			Schedulable schedulable) {
		RenewalProcess process = (RenewalProcess) schedulable;
		if (!process.isUp()) {
			return;
		}
		recomputeReachabilities(process, time);
	}

	private void recomputeReachabilities(RenewalProcess process, double time) {

		// Source being reached for the first time?
		if (!isReached(fSource)) {
			if (process.id() == fSource && process.isUp()) {
				snapshotUptimes();
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
		fUptimeReached[node] = fParent.process(node).uptime(fParent);
		fReachedCount++;
		if (fSampler != null) {
			fSampler.reached(node, this);
		}

		if (isDone()) {
			fParent.unbound(this);
		}
	}

	private void snapshotUptimes() {
		for (int i = 0; i < fUptimeSnapshot.length; i++) {
			fUptimeSnapshot[i] = fParent.process(i).uptime(fParent);
		}
	}

	private boolean isUp(int node) {
		return fCloudNodes[node] || fParent.process(node).isUp();
	}

	boolean isReached(int node) {
		return !Double.isNaN(fReached[node]);
	}

	@Override
	public boolean isDone() {
		return fReachedCount == fReached.length;
	}

	public double perceivedDelay(int i) {
		return fUptimeReached[i] - fUptimeSnapshot[i];
	}

	public double reachTime(int i) {
		return fReached[i] - fReached[fSource];
	}

	public int reachedFrom(int i) {
		return fReachedFrom[i];
	}

	public ActivationSampler getActivationSampler() {
		return fSampler;
	}

	INetwork sim() {
		return fParent;
	}

	@Override
	public boolean isBinding() {
		return true;
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
