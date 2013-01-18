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

/**
 * HFloodSM is the single-message version of the dissemination protocol. Each
 * {@link HFloodSM} instance is associated to a single message at a time.
 * 
 * @author giuliano
 */
public class HFloodSM implements ICyclicProtocol {

	// -------------------------------------------------------------------------
	// Protocol config.
	// -------------------------------------------------------------------------
	private final IndexedNeighborGraph fGraph;

	private final IProtocolReference<HFloodSM> fReference;

	private final IPeerSelector fSelector;

	private final ILiveTransformer fTransformer;

	private final IProcess fProcess;

	// -------------------------------------------------------------------------
	// Protocol state.
	// -------------------------------------------------------------------------
	private BitSet fHistory;

	private BitSet fMappedHistory;

	private IMessage fMessage;

	private State fState = State.IDLE;

	// -------------------------------------------------------------------------
	// Delay metrics.
	// -------------------------------------------------------------------------
	private double fEndToEndDelay;

	private double fRawReceiverDelay;

	public HFloodSM(IndexedNeighborGraph graph, IPeerSelector selector,
			IProcess process, ILiveTransformer transformer,
			IProtocolReference<HFloodSM> reference) {
		fGraph = graph;
		fHistory = new BitSet();
		fMappedHistory = new BitSet();
		fSelector = selector;
		fTransformer = transformer;
		fProcess = process;
		fReference = reference;

		setMessage(null, null);
	}

	/**
	 * Allows external clients to change the message being handled by this
	 * protocol. Note that this simply 'binds' this instance to a particular
	 * message -- the protocol won't start disseminating until the message
	 * actually reaches it.<BR>
	 * <BR>
	 * To start dissemination from here, you need to call
	 * {@link #markReached(IClockData)}.
	 * 
	 * @param message
	 *            the new message to be propagated.
	 * @param initial
	 *            the initial history that came with it.
	 */
	public void setMessage(IMessage message, BitSet initial) {
		fHistory.clear();
		fMappedHistory.clear();

		if (initial != null) {
			fHistory.or(initial);
		}

		fMessage = message;
		fEndToEndDelay = Double.NEGATIVE_INFINITY;
		fRawReceiverDelay = Double.NEGATIVE_INFINITY;

		if (message != null) {
			changeState(State.IDLE);
		}
	}

	/**
	 * Allows external clients to determine when this protocol instance is
	 * reached by the message it's propagating.
	 * 
	 * @param clock
	 */
	public void markReached(IClockData clock) {
		if (!isReached() && fState != State.DONE) {
			fEndToEndDelay = clock.time();
			fRawReceiverDelay = fProcess.uptime(clock);
			fHistory.set(id());
			changeState(State.ACTIVE);
		} else {
			duplicateReceived(clock);
		}
	}

	public IndexedNeighborGraph graph() {
		return fGraph;
	}

	public int id() {
		return fProcess.id();
	}

	/**
	 * Halts dissemination at this protocol instance. If the protocol hasn't
	 * been started yet, it will simply not react when the update finally
	 * arrives.
	 * 
	 * @param engine
	 */
	public void stop() {
		changeState(State.DONE);
	}

	@Override
	public void nextCycle(ISimulationEngine state, IProcess process) {

		INetwork network = state.network();

		// Are we done, down, or not reached yet?
		if (fState == State.DONE || !network.process(id()).isUp()
				|| !fHistory.get(id())) {
			return;
		}

		// Tries to get a peer.
		int neighborId = selectPeer(network);

		// No live neighbors at the moment, so we have to wait.
		if (neighborId < 0) {
			changeState(State.WAITING);
			return;
		}

		// Otherwise we're active.
		changeState(State.ACTIVE);

		// Sends the message and gets the feedback.
		HFloodSM neighbor = fReference.get(this, network, neighborId);
		fHistory.or(neighbor.sendMessage(this, fHistory, state.clock()));

		checkDone();
	}

	private void changeState(State state) {
		fState = state;
	}

	private void checkDone() {
		if (fState == State.DONE) {
			return;
		}

		// Checks if all of our neighbors have gotten the message,
		// including us.
		if (!fHistory.get(id())) {
			return;
		}

		for (int i = 0; i < fGraph.degree(id()); i++) {
			if (!fHistory.get(fGraph.getNeighbor(id(), i))) {
				return;
			}
		}

		changeState(State.DONE);
	}

	private int selectPeer(INetwork sim) {
		Triplet<AbstractIDMapper, INetwork, IndexedNeighborGraph> transform = fTransformer
				.live(fGraph, sim);

		if (transform == null) {
			return fSelector.selectPeer(id(), fGraph, fHistory, sim);
		}

		IDMapper mapper = transform.a;
		int selected = fSelector.selectPeer(mapper.map(id()), transform.c,
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

	protected void duplicateReceived(IClockData clock) {
		// To be overridden by subclasses.
	}

	protected BitSet sendMessage(HFloodSM sender, BitSet history,
			IClockData clock) {
		markReached(clock);
		fHistory.or(history);
		checkDone();
		return fHistory;
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

	public IMessage message() {
		return fMessage;
	}

	@Override
	public State getState() {
		return fState;
	}

}
