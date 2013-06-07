package it.unitn.disi.churn.diffusion;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Random;

import it.unitn.disi.churn.diffusion.graph.ILiveTransformer;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.simulator.core.IClockData;
import it.unitn.disi.simulator.core.INetwork;
import it.unitn.disi.simulator.core.IProcess;
import it.unitn.disi.simulator.core.IReference;
import it.unitn.disi.simulator.core.ISimulationEngine;
import it.unitn.disi.simulator.protocol.ICyclicProtocol;
import it.unitn.disi.simulator.protocol.PausingCyclicProtocolRunner;
import it.unitn.disi.simulator.protocol.PeriodicAction;

/**
 * {@link DisseminationServiceImpl} knows how to disseminate messages. Ideally
 * this should glue together the different disseminaton protocols and a
 * "database" component who can tell which are the known messages. Currently,
 * however, it's a mix between the former and the latter, with protocols
 * themselves keeping track of which messages are known and which are not.
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

	private final int fQuenchDesync;

	private int fQuenchRound;

	private State fState;

	private IndexedNeighborGraph fGraph;

	private BroadcastTracker fCollector = new BroadcastTracker();

	private HFloodSM[] fPushProtocols = new HFloodSM[2];

	private final Antientropy fAntientropy;

	private ArrayList<IMessageObserver> fObservers = new ArrayList<IMessageObserver>();

	private ArrayList<IBroadcastObserver> fBcastObservers = new ArrayList<IBroadcastObserver>();

	public DisseminationServiceImpl(int pid, Random rnd,
			IndexedNeighborGraph graph, IPeerSelector selector,
			IProcess process, ILiveTransformer transformer,
			PausingCyclicProtocolRunner<? extends ICyclicProtocol> runner,
			IReference<ISimulationEngine> engine, boolean oneShot,
			int quenchDesync, double maxQuenchAge, double pushTimeout,
			double antientropyCycle, double antientropyDelay,
			int antientropyPrio) {

		fPushProtocols[UPDATE] = new HFloodUP(graph, selector, process,
				transformer, this, pushTimeout);

		fPushProtocols[NO_UPDATE] = new HFloodNUP(graph, selector, process,
				transformer, this, pushTimeout);

		fAntientropy = new Antientropy(engine, rnd, process.id(),
				antientropyPrio, antientropyCycle, antientropyDelay);

		fGraph = graph;

		fRunner = runner;
		fPid = pid;
		fProcess = process;
		fOneShot = oneShot;
		fMaxQuenchAge = maxQuenchAge;
		fQuenchDesync = quenchDesync;
		fQuenchRound = 0;
	}

	public PeriodicAction antientropy() {
		return fAntientropy;
	}

	public void post(IMessage genericMessage, ISimulationEngine engine) {
		HFloodMMsg message = (HFloodMMsg) genericMessage;

		if (message.isNUP()) {
			fPushProtocols[NO_UPDATE].setMessage(message, null);
			fPushProtocols[NO_UPDATE].markReached(engine.clock());
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
			fPushProtocols[UPDATE].markReached(engine.clock());
		}

		if (fRunner != null) {
			fRunner.wakeUp();
		}
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
		fPushProtocols[UPDATE].nextCycle(engine, process);
		if (fQuenchRound == 0) {
			if (shouldStopQuench(engine)) {
				fPushProtocols[NO_UPDATE].stop();
			}
			fPushProtocols[NO_UPDATE].nextCycle(engine, process);
			fQuenchRound = fQuenchDesync;
		}

		fQuenchRound--;
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

	private void messageReceived(HFloodMMsg message, IClockData clock,
			boolean duplicate) {
		for (IMessageObserver observer : fObservers) {
			observer.messageReceived(fProcess, message, clock, duplicate);
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
				IClockData clock) {
			HFloodMMsg message = (HFloodMMsg) sender.message();
			if (!message.isNUP()) {
				throw new IllegalStateException(message + " != "
						+ this.message());
			}

			// If we got here, tries to replace the message.
			this.setMessage(message, history);
			this.markReached(clock);
			return history;
		}

		@Override
		public void setMessage(IMessage message, BitSet history) {
			if (message() == null
					|| message.timestamp() > message().timestamp()) {
				super.setMessage(message, history);
			}
		}

	}

	class HFloodUP extends HFloodSM {

		public HFloodUP(IndexedNeighborGraph graph, IPeerSelector selector,
				IProcess process, ILiveTransformer transformer,
				IProtocolReference<HFloodSM> reference, double timeout) {
			super(graph, selector, process, transformer, reference, timeout);
		}

		public void rearm() {
			super.setMessage(null, null);
		}

		@Override
		public void markReached(IClockData clock) {
			boolean duplicate = isReached();
			super.markReached(clock);
			messageReceived((HFloodMMsg) message(), clock, duplicate);

			if (!duplicate) {
				HFloodMMsg msg = (HFloodMMsg) message();
				if (msg.getTracker() != null) {
					msg.getTracker().decrement(clock.engine());
				}
			}
		}
	}

	private static final BitSet EMPTY = new BitSet();

	class Antientropy extends PeriodicAction {

		private static final long serialVersionUID = -7825255354837349538L;

		private final IPeerSelector fSelector;

		private final double fPeriod;

		public Antientropy(IReference<ISimulationEngine> engine, Random rnd,
				int id, int prio, double period, double initialDelay) {
			super(engine, prio, id, initialDelay);
			fPeriod = period;
			fSelector = new RandomSelector(rnd);
		}

		@Override
		protected double performAction(ISimulationEngine engine) {
			INetwork network = engine.network();
			IProcess peer = selectPeer(network);
			if (peer != null) {
				DisseminationServiceImpl pair = (DisseminationServiceImpl) peer
						.getProtocol(fPid);

				pair.fPushProtocols[UPDATE].pullSend(engine, fProcess.id());
				pair.fPushProtocols[NO_UPDATE].pullSend(engine, fProcess.id());
			}

			return engine.clock().rawTime() + fPeriod;
		}

		private IProcess selectPeer(INetwork network) {
			int neighbor = fSelector.selectPeer(fProcess.id(), fGraph, EMPTY,
					network);

			if (neighbor == IPeerSelector.NO_LIVE_PEER) {
				return null;
			}

			// This can't be a return code as there are no forbidden peers.
			if (neighbor == IPeerSelector.NO_PEER) {
				throw new IllegalStateException();
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
