package it.unitn.disi.churn.diffusion;

import java.util.BitSet;

import it.unitn.disi.churn.diffusion.churn.ILiveTransformer;
import it.unitn.disi.churn.simulator.IEventObserver;
import it.unitn.disi.churn.simulator.INetwork;
import it.unitn.disi.churn.simulator.ICyclicProtocol;
import it.unitn.disi.churn.simulator.CyclicProtocolRunner;
import it.unitn.disi.churn.simulator.IProcess;
import it.unitn.disi.churn.simulator.Schedulable;
import it.unitn.disi.churn.simulator.SimpleEDSim;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.utils.AbstractIDMapper;
import it.unitn.disi.utils.IDMapper;
import it.unitn.disi.utils.collections.Triplet;

public class HFlood implements ICyclicProtocol {

	private final IndexedNeighborGraph fGraph;

	private final int fId;

	private double fReachTime;

	private BitSet fHistory;

	private BitSet fMappedHistory;

	private final IPeerSelector fSelector;

	private final ILiveTransformer fTransformer;

	private State fState = State.WAITING;

	public HFlood(IndexedNeighborGraph graph,
			IPeerSelector selector, ILiveTransformer transformer, int id) {
		fId = id;
		fGraph = graph;
		fHistory = new BitSet();
		fMappedHistory = new BitSet();
		fSelector = selector;
		fTransformer = transformer;

		clearState();
	}

	public void clearState() {
		fHistory.clear();
		fReachTime = Double.NEGATIVE_INFINITY;
	}

	@Override
	public void nextCycle(double time, INetwork sim,
			CyclicProtocolRunner<? extends ICyclicProtocol> protocols) {

		// Are we done, down, or not reached yet?
		if (fState == State.DONE || !sim.process(fId).isUp()
				|| !fHistory.get(fId)) {
			return;
		}

		// Tries to get a peer.
		int neighborId = selectPeer(sim);

		// No live neighbors at the moment, so we have to wait.
		if (neighborId < 0) {
			fState = State.WAITING;
			return;
		}
		
		// Otherwise we're active.
		fState = State.ACTIVE;

		// Sends the message and gets the feedback.
		HFlood neighbor = (HFlood) protocols
				.get(neighborId);
		fHistory.or(neighbor.sendMessage(fHistory, time));

		checkDone();
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

	private BitSet sendMessage(BitSet history, double time) {
		reached(time);
		fHistory.or(history);
		checkDone();
		return fHistory;
	}

	public void reached(double time) {
		if (fReachTime == Double.NEGATIVE_INFINITY) {
			fReachTime = time;
			fHistory.set(fId);
			fState = State.ACTIVE;
		}
	}

	public double latency() {
		return fReachTime;
	}

	@Override
	public State getState() {
		return fState;
	}

	/**
	 * If we want to make this node the source, we should register the protocol
	 * as a listener to state changes for the network.
	 * 
	 * @return an {@link IEventObserver} which will mark the enclosing
	 *         {@link HFlood} instance as "reached" as soon as the
	 *         {@link IProcess} with the sharing id logs in for the first time.
	 */
	public IEventObserver sourceEventObserver() {
		return new IEventObserver() {

			private SimpleEDSim fParent;

			@Override
			public void stateShifted(INetwork parent, double time,
					Schedulable schedulable) {
				IProcess process = (IProcess) schedulable;
				if (process.id() == fId && process.isUp()) {
					reached(time);
					fParent.done(this);
				}
			}

			@Override
			public void simulationStarted(SimpleEDSim parent) {
				fParent = parent;
				stateShifted(parent, parent.currentTime(), parent.process(fId));
			}

			@Override
			public boolean isDone() {
				return fHistory.get(fId);
			}

			@Override
			public boolean isBinding() {
				return true;
			}
		};
	}

}
