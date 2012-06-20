package it.unitn.disi.churn.diffusion;

import it.unitn.disi.churn.diffusion.graph.ILiveTransformer;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.simulator.core.IClockData;
import it.unitn.disi.simulator.core.INetwork;
import it.unitn.disi.simulator.core.IProcess;
import it.unitn.disi.simulator.core.ISimulationEngine;
import it.unitn.disi.simulator.protocol.ICyclicProtocol;
import it.unitn.disi.utils.AbstractIDMapper;
import it.unitn.disi.utils.IDMapper;
import it.unitn.disi.utils.collections.Triplet;

import java.util.BitSet;

public class HFlood implements ICyclicProtocol {

	private final IndexedNeighborGraph fGraph;

	private final int fId;

	private final int fPid;

	private double fEndToEndDelay;

	private double fRawReceiverDelay;

	private BitSet fHistory;

	private BitSet fMappedHistory;

	private final IPeerSelector fSelector;

	private final ILiveTransformer fTransformer;

	private final IProcess fProcess;

	private State fState = State.WAITING;

	public HFlood(IndexedNeighborGraph graph, IPeerSelector selector,
			IProcess process, ILiveTransformer transformer, int id, int pid) {
		fId = id;
		fPid = pid;
		fGraph = graph;
		fHistory = new BitSet();
		fMappedHistory = new BitSet();
		fSelector = selector;
		fTransformer = transformer;
		fProcess = process;

		clearState();
	}

	public void clearState() {
		fHistory.clear();
		fEndToEndDelay = Double.NEGATIVE_INFINITY;
		fRawReceiverDelay = Double.NEGATIVE_INFINITY;
	}

	@Override
	public void nextCycle(ISimulationEngine state, IProcess process) {

		INetwork network = state.network();

		// Are we done, down, or not reached yet?
		if (fState == State.DONE || !network.process(fId).isUp()
				|| !fHistory.get(fId)) {
			return;
		}

		// Tries to get a peer.
		int neighborId = selectPeer(network);

		// No live neighbors at the moment, so we have to wait.
		if (neighborId < 0) {
			fState = State.WAITING;
			return;
		}

		// Otherwise we're active.
		fState = State.ACTIVE;

		// Sends the message and gets the feedback.
		HFlood neighbor = (HFlood) network.process(neighborId)
				.getProtocol(fPid);
		fHistory.or(neighbor.sendMessage(fHistory, state.clock()));

		checkDone();
	}

	public int id() {
		return fId;
	}

	public IndexedNeighborGraph graph() {
		return fGraph;
	}

	private void checkDone() {
		if (fState == State.DONE) {
			return;
		}

		// Checks if all of our neighbors have gotten the message,
		// including us.
		if (!fHistory.get(fId)) {
			return;
		}

		for (int i = 0; i < fGraph.degree(fId); i++) {
			if (!fHistory.get(fGraph.getNeighbor(fId, i))) {
				return;
			}
		}

		fState = State.DONE;
	}

	private int selectPeer(INetwork sim) {
		Triplet<AbstractIDMapper, INetwork, IndexedNeighborGraph> transform = fTransformer
				.live(fGraph, sim);

		if (transform == null) {
			return fSelector.selectPeer(fId, fGraph, fHistory, sim);
		}

		IDMapper mapper = transform.a;
		int selected = fSelector.selectPeer(mapper.map(fId), transform.c,
				remapHistory(mapper, fHistory), transform.b);

		return selected >= 0 ? mapper.reverseMap(selected) : selected;
	}

	private BitSet remapHistory(IDMapper mapper, BitSet history) {
		fMappedHistory.clear();
		for (int i = history.nextSetBit(0); i >= 0; i = history
				.nextSetBit(i + 1)) {
			if (mapper.isMapped(i)) {
				fMappedHistory.set(mapper.map(i));
			}
		}
		return fMappedHistory;
	}

	private BitSet sendMessage(BitSet history, IClockData clock) {
		markReached(clock);
		fHistory.or(history);
		checkDone();
		return fHistory;
	}

	public void markReached(IClockData clock) {
		if (!isReached()) {
			fEndToEndDelay = clock.time();
			fRawReceiverDelay = fProcess.uptime(clock);
			fHistory.set(fId);
			fState = State.ACTIVE;
		}
	}

	public boolean isReached() {
		return fEndToEndDelay != Double.NEGATIVE_INFINITY;
	}

	public double rawEndToEndDelay() {
		return fEndToEndDelay;
	}

	public double rawReceiverDelay() {
		return fRawReceiverDelay;
	}

	@Override
	public State getState() {
		return fState;
	}

}
