package it.unitn.disi.churn.diffusion;

import java.util.BitSet;

import it.unitn.disi.churn.simulator.INetwork;
import it.unitn.disi.churn.simulator.ICyclicProtocol;
import it.unitn.disi.churn.simulator.CyclicProtocolRunner;
import it.unitn.disi.graph.IndexedNeighborGraph;

public class HistoryForwarding implements ICyclicProtocol {

	private final IndexedNeighborGraph fGraph;

	private final int fId;

	private double fReachTime;

	private BitSet fHistory;

	private final IPeerSelector fSelector;

	private boolean fDone;

	public HistoryForwarding(IndexedNeighborGraph graph,
			IPeerSelector selector, int id) {
		fId = id;
		fGraph = graph;
		fHistory = new BitSet();
		fSelector = selector;
		
		clearState();
	}

	public void clearState() {
		fHistory.clear();
		fReachTime = Double.NEGATIVE_INFINITY;
	}

	@Override
	public void nextCycle(double time, INetwork sim,
			CyclicProtocolRunner protocols) {
		// Are we done, or not reached yet?
		if (fDone || !fHistory.get(fId)) {
			return;
		}

		// Tries to get a peer.
		int neighborId = selectPeer(sim);

		// No more neighbors to send to, we're done.
		if (neighborId == IPeerSelector.NO_PEER) {
			fDone = true;
			return;
		}

		// No live neighbors at the moment. Wait.
		if (neighborId == IPeerSelector.NO_LIVE_PEER) {
			return;
		}

		// Sends the message and gets the feedback.
		HistoryForwarding neighbor = (HistoryForwarding) protocols
				.get(neighborId);
		fHistory.or(neighbor.sendMessage(fHistory, time));
	}

	private int selectPeer(INetwork sim) {
		return fSelector.selectPeer(fId, fGraph, fHistory, sim);
	}

	private BitSet sendMessage(BitSet history, double time) {
		reached(time);
		fHistory.or(history);
		return fHistory;
	}

	public void reached(double time) {
		if (fReachTime == Double.NEGATIVE_INFINITY) {
			fReachTime = time;
			fHistory.set(fId);
		}
	}
	
	public double latency() {
		return fReachTime;
	}

	public boolean isDone() {
		return fDone;
	}

}
