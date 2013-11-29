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
	// Flags describing a message received by the protocol when it calls the
	// #messageReceived hook.
	// -------------------------------------------------------------------------
	/**
	 * Toggled if the message is a duplicate
	 */
	public static final int DUPLICATE = 1;

	public static final int ANTIENTROPY_PUSH = 2;

	/**
	 * Toggled if message received by a call to
	 * {@link #antientropyRespond(ISimulationEngine, int)}.
	 */
	public static final int ANTIENTROPY_PULL = 4;

	/**
	 * Toggled when node reached from means other than the P2P network.
	 */
	public static final int NO_MESSAGE = 8;

	// -------------------------------------------------------------------------
	// Protocol config.
	// -------------------------------------------------------------------------
	private final IndexedNeighborGraph fGraph;

	private final IProtocolReference<HFloodSM> fReference;

	private final IPeerSelector fSelector;

	private final ILiveTransformer fTransformer;

	private final IProcess fProcess;

	private final double fTimeout;

	// -------------------------------------------------------------------------
	// Protocol state.
	// -------------------------------------------------------------------------
	private BitSet fHistory;

	private BitSet fMappedHistory;

	private IMessage fMessage;

	private State fState = State.IDLE;

	private double fLastRound;

	// -------------------------------------------------------------------------
	// Delay and status metrics.
	// -------------------------------------------------------------------------
	private double fEndToEndDelay;

	private double fRawReceiverDelay;

	private int fContactsInitiated;

	private int fContactsReceived;

	public HFloodSM(IndexedNeighborGraph graph, IPeerSelector selector,
			IProcess process, ILiveTransformer transformer,
			IProtocolReference<HFloodSM> reference) {
		this(graph, selector, process, transformer, reference,
				Double.POSITIVE_INFINITY);
	}

	public HFloodSM(IndexedNeighborGraph graph, IPeerSelector selector,
			IProcess process, ILiveTransformer transformer,
			IProtocolReference<HFloodSM> reference, double timeout) {
		fGraph = graph;
		fHistory = new BitSet();
		fMappedHistory = new BitSet();
		fSelector = selector;
		fTransformer = transformer;
		fProcess = process;
		fReference = reference;
		fTimeout = timeout;

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
	public void markReached(int sender, IClockData clock, int flags) {

		boolean duplicate = isReached();

		if (!duplicate) {
			fEndToEndDelay = clock.time();
			fRawReceiverDelay = fProcess.uptime(clock);
			fHistory.set(id());

			if (fState != State.DONE) {
				// Register time when we first activate.
				fLastRound = clock.rawTime();
				changeState(State.ACTIVE);
			}
		}

		// If message is duplicate and antientropy, it's just an artefact and
		// we should not count it.
		if (!duplicate || !isAntientropy(flags)) {
			messageReceived(sender, clock, duplicate ? flags | DUPLICATE
					: flags);
		}

	}

	private boolean isAntientropy(int flags) {
		return (flags & ANTIENTROPY_PULL) != 0
				|| (flags & ANTIENTROPY_PUSH) != 0;
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
	public void nextCycle(ISimulationEngine engine, IProcess process) {
		INetwork network = engine.network();

		// Are we done, down, or not reached yet?
		if (fState == State.DONE || !network.process(id()).isUp()
				|| !fHistory.get(id())) {
			return;
		}

		if (timedOut(engine)) {
			changeState(State.DONE);
			return;
		}

		// Tries to get a peer.
		int neighborId = selectPeer(network);

		// No live neighbors at the moment, so we have to wait.
		if (neighborId == IPeerSelector.NO_LIVE_PEER
				|| neighborId == IPeerSelector.NO_PEER) {
			changeState(State.WAITING);
			return;
		}

		// Otherwise we're active.
		changeState(State.ACTIVE);

		// Updates last active round.
		fLastRound = engine.clock().rawTime();

		// Only sends a message if we actually have a peer to send to.
		if (neighborId != IPeerSelector.SKIP_ROUND) {
			fContactsInitiated++;
			fReference.get(this, network, neighborId).fContactsReceived++;
			doSend(engine, network, neighborId, 0);
		}
	}

	private boolean timedOut(ISimulationEngine engine) {
		return (engine.clock().rawTime() - fLastRound) > fTimeout;
	}

	/**
	 * Allows implementation of antientropy without having to properly refactor
	 * the code. :-)
	 * 
	 * This method essentially forces this protocol to send the update (if this
	 * node has it) to a calling neighbor, but by performing some sanity checks.
	 * 
	 * TODO to make this right, we need to separate message storage (who has and
	 * who doesn't have the message) from protocol instances, and we need proper
	 * storage/protocol interfaces.
	 * 
	 * @param engine
	 *            current simulation engine.
	 * 
	 * @param initiator
	 *            id of the pulling node.
	 * 
	 * @return <code>true</code> if this node had something to send back, or
	 *         <code>false</code> otherwise.
	 */
	public boolean antientropy(ISimulationEngine engine, int initiator,
			boolean pull) {
		INetwork network = engine.network();

		if (!network.process(id()).isUp()) {
			throw new IllegalStateException("Can't pull from dead neighbor.");
		}

		int type = pull ? ANTIENTROPY_PULL : ANTIENTROPY_PUSH;

		if (!fHistory.get(id())) {
			return false;
		}

		doSend(engine, network, initiator, type);

		return true;
	}

	private void doSend(ISimulationEngine state, INetwork network,
			int neighborId, int flags) {

		// Sends the message and gets the feedback.
		HFloodSM neighbor = fReference.get(this, network, neighborId);
		fHistory.or(neighbor.sendMessage(this, fHistory, state.clock(), flags));

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

	protected void messageReceived(int sender, IClockData clock, int flags) {
		// To be overridden by subclasses.
	}

	protected BitSet sendMessage(HFloodSM sender, BitSet history,
			IClockData clock, int flags) {
		markReached(sender.id(), clock, flags);
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

	public int contactsInitiated() {
		return fContactsInitiated;
	}

	public int contactsReceived() {
		return fContactsReceived;
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
