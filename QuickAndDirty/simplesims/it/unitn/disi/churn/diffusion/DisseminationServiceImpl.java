package it.unitn.disi.churn.diffusion;

import it.unitn.disi.churn.diffusion.graph.ILiveTransformer;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.simulator.core.IClockData;
import it.unitn.disi.simulator.core.INetwork;
import it.unitn.disi.simulator.core.IProcess;
import it.unitn.disi.simulator.core.IReference;
import it.unitn.disi.simulator.core.ISimulationEngine;
import it.unitn.disi.simulator.core.Schedulable;
import it.unitn.disi.simulator.protocol.ICyclicProtocol;
import it.unitn.disi.simulator.protocol.PausingCyclicProtocolRunner;
import it.unitn.disi.simulator.protocol.PeriodicAction;
import it.unitn.disi.utils.ResettableCounter;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Random;

/**
 * {@link DisseminationServiceImpl} knows how to disseminate messages. Ideally
 * this should glue together the different disseminaton protocols and a
 * "database" component who can tell which are the known messages. Currently,
 * however, it's a mix between the former and the latter, with protocols
 * themselves keeping track of which messages are known and which are not.
 * 
 * The current implementation is hacky, and strongly based on the assumption
 * that a single update message circulates at a time. Changing this assumption
 * will require a completely rewritten implementation.
 * 
 * @author giuliano
 */
public class DisseminationServiceImpl implements ICyclicProtocol,
		IProtocolReference<HFloodSM>, IDisseminationService {

	private static final int UPDATE = 0;

	private static final int NO_UPDATE = 1;

	private final PausingCyclicProtocolRunner<? extends ICyclicProtocol> fRunner;

	private final int fPid;

	private final IProcess fProcess;

	private final boolean fOneShot;

	private final double fMaxQuenchAge;
	
	private boolean fOnDemandPull;

	private int fLastKnown = -1;

	private State fState;

	private BitSet fStaticBlacklist;

	private IndexedNeighborGraph fGraph;

	private BroadcastTracker fCollector = new BroadcastTracker();

	private HFloodSM[] fPushProtocols = new HFloodSM[2];

	private final Antientropy fAntientropy;

	private final MessageStatistics fStats;

	private ArrayList<IMessageObserver> fObservers = new ArrayList<IMessageObserver>();

	private ArrayList<IBroadcastObserver> fBcastObservers = new ArrayList<IBroadcastObserver>();

	public DisseminationServiceImpl(int pid, Random rnd,
			IndexedNeighborGraph graph, IPeerSelector pushUpdateSelector,
			IPeerSelector pushQuenchSelector, IPeerSelector aeSelector,
			IProcess process, ILiveTransformer transformer,
			PausingCyclicProtocolRunner<? extends ICyclicProtocol> runner,
			IReference<ISimulationEngine> engine, boolean oneShot,
			double maxQuenchAge, double updatePushTimeout,
			double quenchPushTimeout, double antientropyShortCycle,
			double antientropyLongCycle, double antientropyDelay,
			BitSet antientropyStaticblacklist, int antientropyShortRounds,
			int antientropyPrio, boolean aeBlacklist, boolean onDemandPull,
			// For debugging.
			MessageStatistics stats) {

		fPushProtocols[UPDATE] = new HFloodUP(graph, pushUpdateSelector,
				process, transformer, this, updatePushTimeout);

		fPushProtocols[NO_UPDATE] = new HFloodNUP(graph, pushQuenchSelector,
				process, transformer, this, quenchPushTimeout);

		fAntientropy = new Antientropy(engine, aeSelector, process.id(),
				antientropyPrio, antientropyShortCycle, antientropyLongCycle,
				antientropyShortRounds, antientropyDelay, aeBlacklist);

		fStats = stats;
		fGraph = graph;
		fRunner = runner;
		fPid = pid;
		fProcess = process;
		fOneShot = oneShot;
		fMaxQuenchAge = maxQuenchAge;
		fStaticBlacklist = antientropyStaticblacklist;
	}

	public PeriodicAction antientropy() {
		return fAntientropy;
	}

	public void post(IMessage genericMessage, ISimulationEngine engine) {
		HFloodMMsg message = (HFloodMMsg) genericMessage;

		if (message.isNUP()) {
			fPushProtocols[NO_UPDATE].setMessage(message, null);
			fPushProtocols[NO_UPDATE].markReached(fProcess.id(),
					engine.clock(), HFloodSM.NO_MESSAGE);
		}

		else {
			if (message.source() == fProcess.id()) {
				if (!fCollector.isBroadcastDone()) {
					throw new IllegalStateException(
							"Can't disseminate more than one update at a time.");
				}
				message.setTracker(fCollector);
				fCollector.beginBroadcast(engine, message);
				// Eagerly sets the message to all network participants (it's
				// easier this way).
				INetwork network = engine.network();
				for (int i = 0; i < network.size(); i++) {
					DisseminationServiceImpl hfmm = (DisseminationServiceImpl) network
							.process(i).getProtocol(fPid);
					hfmm.fPushProtocols[UPDATE].setMessage(message, null);
				}
			} else {
				if (fPushProtocols[UPDATE].getState() != State.IDLE) {
					throw new IllegalStateException();
				}
			}
			// Marks the current as reached.
			fPushProtocols[UPDATE].markReached(fProcess.id(), engine.clock(),
					HFloodSM.NO_MESSAGE);
		}

		if (fRunner != null) {
			fRunner.wakeUp();
		}
	}

	public int latestSequence() {
		return fLastKnown;
	}

	@Override
	public void addMessageObserver(IMessageObserver observer) {
		fObservers.add(observer);
	}

	@Override
	public void removeMessageObserver(IMessageObserver observer) {
		fObservers.remove(observer);
	}

	public void addBroadcastObserver(IBroadcastObserver observer) {
		fBcastObservers.add(observer);
	}

	public void removeBroadcastObserver(IBroadcastObserver observer) {
		fBcastObservers.add(observer);
	}

	@Override
	public void nextCycle(ISimulationEngine engine, IProcess process) {

		// XXX Why no check for fState == DONE? Bug?

		fPushProtocols[UPDATE].nextCycle(engine, process);
		if (shouldStopQuench(engine)) {
			fPushProtocols[NO_UPDATE].stop();
		}
		fPushProtocols[NO_UPDATE].nextCycle(engine, process);

		// If dissemination is done, we're done.
		if (fPushProtocols[UPDATE].getState() == State.DONE) {
			fState = fOneShot ? State.DONE : State.IDLE;
		}

		// Otherwise we're either active...
		else if (fPushProtocols[UPDATE].getState() == State.ACTIVE
				|| fPushProtocols[NO_UPDATE].getState() == State.ACTIVE) {
			fState = State.ACTIVE;
		}

		// ... or waiting. Note that if the P2P network is disabled, we're never
		// active. So we're either done, or waiting, and the protocol doesn't
		// run.
		else {
			fState = State.WAITING;
		}
	}

	public void suppressAntientropy() {
		fAntientropy.suppress();
	}

	private boolean shouldStopQuench(ISimulationEngine engine) {
		IMessage msg = fPushProtocols[NO_UPDATE].message();
		if (msg == null) {
			return false;
		}
		double ts = fPushProtocols[NO_UPDATE].message().timestamp();
		return (engine.clock().rawTime() - ts) > fMaxQuenchAge;
	}

	@Override
	public State getState() {
		return fState;
	}

	@Override
	public HFloodSM get(HFloodSM caller, INetwork network, int id) {
		DisseminationServiceImpl neighbor = (DisseminationServiceImpl) network
				.process(id).getProtocol(fPid);
		return neighbor.get((HFloodMMsg) caller.message());
	}

	public HFloodSM get(HFloodMMsg message) {
		return message.isNUP() ? fPushProtocols[NO_UPDATE]
				: fPushProtocols[UPDATE];
	}

	public boolean isReached() {
		return fPushProtocols[UPDATE].isReached();
	}

	private void messageReceived(int sender, int receiver, HFloodMMsg message,
			IClockData clock, int flags) {

		if ((flags & HFloodSM.NO_MESSAGE) != 0) {
			return;
		}

		for (IMessageObserver observer : fObservers) {
			observer.messageReceived(sender, receiver, message, clock, flags);
		}

	}

	class HFloodNUP extends HFloodUP {

		public HFloodNUP(IndexedNeighborGraph graph, IPeerSelector selector,
				IProcess process, ILiveTransformer transformer,
				IProtocolReference<HFloodSM> reference, double timeout) {
			super(graph, selector, process, transformer, reference, timeout);
		}

		@Override
		protected BitSet sendMessage(HFloodSM sender, BitSet history,
				IClockData clock, int flags) {
			HFloodMMsg message = (HFloodMMsg) sender.message();
			if (!message.isNUP()) {
				throw new IllegalStateException(message + " != "
						+ this.message());
			}

			// Old quench messages.
			if (!shouldAccept(message)) {
				if (!isDuplicate(message)) {
					boolean antientropy = (flags & ANTIENTROPY_PULL) != 0;
					antientropy |= (flags & ANTIENTROPY_PUSH) != 0;
					fStats.expiredQuench(
							antientropy ? MessageStatistics.ANTIENTROPY
									: MessageStatistics.HFLOOD, id());
				}
			}

			// Quench is useful, replaces it.
			else {
				this.setMessage(message, history);
			}

			// Marks as reached if message got replaced.
			if (!isReached()) {
				this.markReached(sender.id(), clock, flags);
			}

			// Otherwise have to manually trigger message event as the semantics
			// for detecting duplicate messages is messed up.
			else {
				messageReceived(sender.id(), clock, flags);
			}

			if (fOnDemandPull) {
				// Finally, we trigger on-demand antientropy if we're missing
				// sequence numbers.
				DisseminationServiceImpl diss = (DisseminationServiceImpl) sender
						.process().getProtocol(fPid);
				if (diss.fLastKnown > fLastKnown) {
					// We have to schedule the pull as doing it here would be messy.
					// We update the counter immediately so no more pulls get
					// scheduled. This works fine under the assumptions that:
					//
					// 1. the sender will remain online long enough;
					// 2. we have only one update to download.
					//
					// which is okay for this simple simulation.
					//
					clock.engine().schedule(
							new PullExchange(clock.rawTime(),
									diss.fPushProtocols[UPDATE]));
					fLastKnown = diss.fLastKnown;
				}
			}
			

			return history;
		}

		private boolean isDuplicate(HFloodMMsg message) {
			return message == this.message();
		}

		public boolean shouldAccept(IMessage message) {
			return message() == null
					|| message.timestamp() > message().timestamp();
		}

	}

	class HFloodUP extends HFloodSM {

		public HFloodUP(IndexedNeighborGraph graph, IPeerSelector selector,
				IProcess process, ILiveTransformer transformer,
				IProtocolReference<HFloodSM> reference, double timeout) {
			super(graph, selector, process, transformer, reference, timeout);
		}

		@Override
		public void markReached(int sender, IClockData clock, int flags) {
			/*
			 * We override markReched essentially to decrement the broadcast
			 * tracker counter.
			 */
			boolean duplicate = isReached();
			super.markReached(sender, clock, flags);
			if (!duplicate) {
				HFloodMMsg msg = (HFloodMMsg) message();
				if (msg.getTracker() != null) {
					msg.getTracker().decrement(clock.engine());
				}

				// This works fine because we only deal with one update at
				// a time.
				if(!msg.isNUP()) {
					fLastKnown = msg.sequence;
				}
			}
		}

		@Override
		protected void messageReceived(int sender, IClockData clock, int flags) {
			DisseminationServiceImpl.this.messageReceived(sender,
					fProcess.id(), (HFloodMMsg) message(), clock, flags);
		}
	}

	class PullExchange extends Schedulable {

		private static final long serialVersionUID = 1L;

		private final double fTime;

		private final HFloodSM fPeer;

		private boolean fScheduled = false;

		public PullExchange(double time, HFloodSM peer) {
			fTime = time;
			fPeer = peer;
		}

		@Override
		public boolean isExpired() {
			return fScheduled;
		}

		@Override
		public void scheduled(ISimulationEngine engine) {
			fScheduled = true;
			// Registers the pull request.
			messageReceived(fProcess.id(), fPeer.process().id(), null,
					engine.clock(), HFloodSM.ANTIENTROPY_PUSH
							| HFloodSM.ANTIENTROPY_PULL);

			// Pulls.
			fPeer.antientropy(engine, fProcess.id(), true);
		}

		@Override
		public double time() {
			return fTime;
		}

		@Override
		public int type() {
			return -1;
		}
	}

	class Antientropy extends PeriodicAction {

		private static final long serialVersionUID = -7825255354837349538L;

		private final IPeerSelector fSelector;

		private final double fShortPeriod;

		private final double fLongPeriod;

		private final BitSet fSessionBlacklist;

		private final boolean fBlacklistSessions;

		private boolean fSuppressed;

		private ResettableCounter fShortRounds;

		public Antientropy(IReference<ISimulationEngine> engine,
				IPeerSelector peerSelector, int id, int prio,
				double shortPeriod, double longPeriod, int shortRounds,
				double initialDelay, boolean blacklistSessions) {

			super(engine, prio, id, initialDelay);

			fShortPeriod = shortPeriod;
			fLongPeriod = longPeriod;
			fShortRounds = new ResettableCounter(shortRounds);

			fSelector = peerSelector;
			fSessionBlacklist = new BitSet();
			fBlacklistSessions = blacklistSessions;
		}

		@Override
		public void eventPerformed(ISimulationEngine state,
				Schedulable schedulable, double nextShift) {

			// Clear all per-session state.
			fSessionBlacklist.clear();
			fSessionBlacklist.or(fStaticBlacklist);
			fShortRounds.reset();
			fSuppressed = false;

			super.eventPerformed(state, schedulable, nextShift);
		}

		public void suppress() {
			fSuppressed = true;
		}

		@Override
		protected double performAction(ISimulationEngine engine) {
			// XXX this is not the most efficient way of implementing
			// suppression (though it is probably the simplest), as the periodic
			// action will be needlessly rescheduled.
			if (!fSuppressed) {
				doExchange(engine);
			}

			return engine.clock().rawTime()
					+ ((fShortRounds.get() > 0) ? fShortPeriod : fLongPeriod);
		}

		private void doExchange(ISimulationEngine engine) {
			INetwork network = engine.network();
			IProcess peer = selectPeer(network);

			// This antientropy implementation is really a hack: what it does is
			// to force a push protocol exchange, which in the end is okay if
			// we're only disseminating one message.
			if (peer != null) {

				DisseminationServiceImpl pair = (DisseminationServiceImpl) peer
						.getProtocol(fPid);

				// Forces two-way exchanges.
				/**
				 * XXX Message counts will be OVERESTIMATED if nodes are
				 * disseminating updates frequently as the current counting
				 * mechanism is not able to deal with aggregate messages. An
				 * exchange involving a QUENCH and an UPDATE, therefore, will
				 * count as two separate messages.
				 */
				// Registers Antientropy exchange.
				messageReceived(fProcess.id(), pair.fProcess.id(), null,
						engine.clock(), HFloodSM.ANTIENTROPY_PUSH
								| HFloodSM.ANTIENTROPY_PULL);

				// XXX this is totally not cool, and it's an abuse of
				// messageReceived.
				messageReceived(pair.fProcess.id(), fProcess.id(), null,
						engine.clock(), HFloodSM.ANTIENTROPY_PUSH
								| HFloodSM.ANTIENTROPY_PULL);

				// PUSH
				fPushProtocols[UPDATE].antientropy(engine, peer.id(), false);
				// PUSH
				fPushProtocols[NO_UPDATE].antientropy(engine, peer.id(), false);

				// PULL
				pair.fPushProtocols[UPDATE].antientropy(engine, fProcess.id(),
						true);
				// PULL
				pair.fPushProtocols[NO_UPDATE].antientropy(engine,
						fProcess.id(), true);
			}

			// Short rounds count even if we cannot select a peer.
			fShortRounds.decrement();
		}

		private IProcess selectPeer(INetwork network) {
			int neighbor = fSelector.selectPeer(fProcess.id(), fGraph,
					fSessionBlacklist, network);

			if (neighbor < 0) {
				return null;
			}

			if (fBlacklistSessions) {
				fSessionBlacklist.set(neighbor);
			}

			return network.process(neighbor);
		}

	}

	class BroadcastTracker {

		private int fIntendedDestinations;

		private HFloodMMsg fMessage;

		public void beginBroadcast(ISimulationEngine engine, HFloodMMsg message) {
			fIntendedDestinations = engine.network().size();
			fMessage = message;

			for (IBroadcastObserver observer : fBcastObservers) {
				observer.broadcastStarted(message, engine);
			}
		}

		public void decrement(ISimulationEngine engine) {
			fIntendedDestinations--;
			if (fIntendedDestinations < 0) {
				throw new IllegalStateException();
			}
			// System.err.println(fIntendedDestinations);
			if (fIntendedDestinations == 0) {
				for (IBroadcastObserver observer : fBcastObservers) {
					observer.broadcastDone(fMessage, engine);
				}
				fMessage = null;
			}
		}

		public boolean isBroadcastDone() {
			return fIntendedDestinations == 0;
		}

	}

	public static interface IBroadcastObserver {

		public void broadcastStarted(HFloodMMsg message,
				ISimulationEngine engine);

		public void broadcastDone(HFloodMMsg message, ISimulationEngine engine);

	}
}
