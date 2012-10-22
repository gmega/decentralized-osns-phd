package it.unitn.disi.churn.diffusion;

import java.util.ArrayList;
import java.util.BitSet;

import it.unitn.disi.churn.diffusion.graph.ILiveTransformer;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.simulator.core.IClockData;
import it.unitn.disi.simulator.core.INetwork;
import it.unitn.disi.simulator.core.IProcess;
import it.unitn.disi.simulator.core.ISimulationEngine;
import it.unitn.disi.simulator.protocol.ICyclicProtocol;
import it.unitn.disi.simulator.protocol.PausingCyclicProtocolRunner;

/**
 * {@link HFloodMM} manages multiple messages by creating multiple instances of
 * {@link HFloodSM}.
 * 
 * @author giuliano
 */
public class HFloodMM implements ICyclicProtocol, IProtocolReference<HFloodSM>,
		IDisseminationService {

	private static final int UPDATE = 0;

	private static final int NO_UPDATE = 1;

	private final PausingCyclicProtocolRunner<? extends ICyclicProtocol> fRunner;

	private State fState;

	private int fPid;

	private IProcess fProcess;

	private boolean fNoP2P;

	private boolean fOneShot;

	private BroadcastTracker fCollector = new BroadcastTracker();

	private HFloodSM[] fProtocols = new HFloodSM[2];

	private ArrayList<IMessageObserver> fObservers = new ArrayList<IMessageObserver>();

	private ArrayList<IBroadcastObserver> fBcastObservers = new ArrayList<IBroadcastObserver>();

	public HFloodMM(int pid, IndexedNeighborGraph graph,
			IPeerSelector selector, IProcess process,
			ILiveTransformer transformer,
			PausingCyclicProtocolRunner<? extends ICyclicProtocol> runner,
			boolean noP2P, boolean oneShot) {

		fProtocols[UPDATE] = new HFloodUP(graph, selector, process,
				transformer, this);

		fProtocols[NO_UPDATE] = new HFloodNUP(graph, selector, process,
				transformer, this);

		fRunner = runner;
		fPid = pid;
		fProcess = process;
		fOneShot = oneShot;
		fNoP2P = noP2P;
	}

	public void post(IMessage genericMessage, ISimulationEngine engine) {
		HFloodMMsg message = (HFloodMMsg) genericMessage;

		if (message.isNUP()) {
			fProtocols[NO_UPDATE].setMessage(message, null);
			fProtocols[NO_UPDATE].markReached(engine.clock());
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
					HFloodMM hfmm = (HFloodMM) network.process(i).getProtocol(
							fPid);
					hfmm.fProtocols[UPDATE].setMessage(message, null);
				}
			} else {
				if (fProtocols[UPDATE].getState() != State.IDLE) {
					throw new IllegalStateException();
				}
			}
			// Marks the current as reached.
			fProtocols[UPDATE].markReached(engine.clock());
		}

		fRunner.wakeUp();
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

		// If the P2P network is enabled, cycles the protocols.
		if (!fNoP2P) {
			for (int i = 0; i < fProtocols.length; i++) {
				fProtocols[i].nextCycle(engine, process);
			}
		}

		// If dissemination is done, we're done.
		if (fProtocols[UPDATE].getState() == State.DONE) {
			fState = fOneShot ? State.DONE : State.IDLE;
		}

		// Otherwise we're either active...
		else if (!fNoP2P
				&& (fProtocols[UPDATE].getState() == State.ACTIVE || fProtocols[NO_UPDATE]
						.getState() == State.ACTIVE)) {
			fState = State.ACTIVE;
		}

		// ... or waiting. Note that if the P2P network is disabled, we're never
		// active. So we're either done, or waiting, and the protocol doesn't
		// run.
		else {
			fState = State.WAITING;
		}
	}

	@Override
	public State getState() {
		return fState;
	}

	@Override
	public HFloodSM get(HFloodSM caller, INetwork network, int id) {
		HFloodMM neighbor = (HFloodMM) network.process(id).getProtocol(fPid);
		return neighbor.get((HFloodMMsg) caller.message());
	}

	public HFloodSM get(HFloodMMsg message) {
		return message.isNUP() ? fProtocols[NO_UPDATE] : fProtocols[UPDATE];
	}

	public boolean isReached() {
		return fProtocols[UPDATE].isReached();
	}

	private void messageReceived(HFloodMMsg message, IClockData clock) {
		for (IMessageObserver observer : fObservers) {
			observer.messageReceived(message, clock);
		}
	}

	class HFloodNUP extends HFloodUP {

		public HFloodNUP(IndexedNeighborGraph graph, IPeerSelector selector,
				IProcess process, ILiveTransformer transformer,
				IProtocolReference<HFloodSM> reference) {
			super(graph, selector, process, transformer, reference);
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
				IProtocolReference<HFloodSM> reference) {
			super(graph, selector, process, transformer, reference);
		}

		public void rearm() {
			super.setMessage(null, null);
		}

		@Override
		public void markReached(IClockData clock) {
			boolean notify = !isReached();
			super.markReached(clock);
			if (notify) {
				messageReceived((HFloodMMsg) message(), clock);
				HFloodMMsg msg = (HFloodMMsg) message();
				if (msg.getTracker() != null) {
					System.err.println("Message reached node " + fProcess.id());
					msg.getTracker().decrement(clock.engine());
				}
			}
		}
	}

	class BroadcastTracker {

		private int fIntendedDestinations;

		private HFloodMMsg fMessage;

		public void beginBroadcast(ISimulationEngine engine, HFloodMMsg message) {
			fIntendedDestinations = engine.network().size();
			fMessage = message;
		}

		public void decrement(ISimulationEngine engine) {
			fIntendedDestinations--;
			System.err.println("Remaining: " + fIntendedDestinations);
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
		public void broadcastDone(HFloodMMsg message, ISimulationEngine engine);
	}
}
