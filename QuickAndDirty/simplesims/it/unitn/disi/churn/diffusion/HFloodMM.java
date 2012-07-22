package it.unitn.disi.churn.diffusion;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.NoSuchElementException;

import it.unitn.disi.churn.diffusion.graph.ILiveTransformer;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.simulator.core.IClockData;
import it.unitn.disi.simulator.core.INetwork;
import it.unitn.disi.simulator.core.IProcess;
import it.unitn.disi.simulator.core.ISimulationEngine;
import it.unitn.disi.simulator.protocol.ICyclicProtocol;
import it.unitn.disi.simulator.protocol.PausingCyclicProtocolRunner;

/**
 * {@link HFloodMM} manages multiple messages by dynamically managing multiple
 * instances of the {@link HFloodSM} protocol.
 * 
 * @author giuliano
 */
public class HFloodMM implements ICyclicProtocol, IProtocolReference<HFloodSM>,
		IDisseminationService {

	private static final int UPDATE = 0;

	private static final int NO_UPDATE = 1;

	private final PausingCyclicProtocolRunner<? extends ICyclicProtocol> fRunner;

	private State fState;

	private HFloodSM[] fProtocols = new HFloodSM[2];

	private ArrayList<IMessageObserver> fObservers = new ArrayList<IMessageObserver>();

	public HFloodMM(int pid, IndexedNeighborGraph graph,
			IPeerSelector selector, IProcess process,
			ILiveTransformer transformer,
			PausingCyclicProtocolRunner<? extends ICyclicProtocol> runner) {

		fProtocols[UPDATE] = new HFloodSM(graph, selector, process,
				transformer, new PIDReference<HFloodSM>(pid));

		fProtocols[NO_UPDATE] = new HFloodSM(graph, selector, process,
				transformer, new PIDReference<HFloodSM>(pid));

		fRunner = runner;
	}

	public void post(Message message, ISimulationEngine engine) {
		HFloodSM instance;
		if (message.isNUP()) {
			instance = fProtocols[NO_UPDATE];
		} else {
			if (fProtocols[UPDATE].getState() != State.IDLE) {
				throw new IllegalStateException();
			}
			instance = fProtocols[UPDATE];
		}

		instance.setMessage(message, null);
		instance.markReached(engine.clock());

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

	@Override
	public void nextCycle(ISimulationEngine engine, IProcess process) {
		// This strategy works here because our protocols don't exchange
		// messages among themselves.
		int done = 0;
		int waiting = 0;

		for (HFloodSM protocol : fProtocols) {
			switch (protocol.getState()) {

			case IDLE:
			case WAITING:
				waiting++;
				break;

			case DONE:
				waiting++;
				done++;
				break;

			case ACTIVE:
				break;

			}
		}

		if (done == fProtocols.length) {
			fState = State.DONE;
		} else if (waiting == fProtocols.length) {
			fState = State.WAITING;
		} else {
			fState = State.ACTIVE;
		}
	}

	@Override
	public State getState() {
		return fState;
	}

	@Override
	public HFloodSM get(HFloodSM caller, INetwork network, int id) {
		return get(caller.message());
	}

	public HFloodSM get(Message message) {
		for (HFloodSM protocol : fProtocols) {
			if (protocol.message() == message) {
				return protocol;
			}
		}
		throw new NoSuchElementException();
	}

	private void messageReceived(Message message, IClockData clock) {
		for (IMessageObserver observer : fObservers) {
			observer.messageReceived(message, clock);
		}
	}

	class HFloodSMInternal extends HFloodSM {

		public HFloodSMInternal(IndexedNeighborGraph graph,
				IPeerSelector selector, IProcess process,
				ILiveTransformer transformer,
				IProtocolReference<HFloodSM> reference) {
			super(graph, selector, process, transformer, reference);
		}

		@Override
		protected BitSet sendMessage(HFloodSM sender, BitSet history,
				IClockData clock) {
			Message message = sender.message();
			if (this.message() == message) {
				return super.sendMessage(sender, history, clock);
			} else if (!message.isNUP()) {
				throw new IllegalStateException();
			}

			// If we got here, tries to replace the message.
			this.setMessage(message, history);
			this.markReached(clock);

			return history;
		}

		@Override
		public void markReached(IClockData clock) {
			boolean notify = !isReached();
			super.markReached(clock);
			if (notify) {
				messageReceived(message(), clock);
			}
		}

		@Override
		public void setMessage(Message message, BitSet history) {
			if (message.timestamp() > message().timestamp()) {
				super.setMessage(message, history);
			}
		}

	}
}
