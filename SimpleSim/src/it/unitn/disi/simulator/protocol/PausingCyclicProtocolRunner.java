package it.unitn.disi.simulator.protocol;

import it.unitn.disi.simulator.core.Binding;
import it.unitn.disi.simulator.core.IClockData;
import it.unitn.disi.simulator.core.INetwork;
import it.unitn.disi.simulator.core.IProcess;
import it.unitn.disi.simulator.core.IEventObserver;
import it.unitn.disi.simulator.core.Schedulable;
import it.unitn.disi.simulator.core.ISimulationEngine;
import it.unitn.disi.simulator.protocol.ICyclicProtocol.State;

/**
 * {@link PausingCyclicProtocolRunner} stops scheduling the cyclic protocol when
 * all nodes go quiescent, until there is a change in the underlying network (a
 * node logs in).
 * 
 * This {@link IEventObserver} pairs with the internal
 * {@link PausingSchedulable}.
 * 
 * @author giuliano
 * 
 * @param <K>
 */
@Binding
public class PausingCyclicProtocolRunner<K extends ICyclicProtocol> extends
		CyclicProtocolRunner<K> {

	private PausingSchedulable fSchedulable;

	public PausingCyclicProtocolRunner(ISimulationEngine engine, double period,
			int type, int pid) {
		super(pid);
		fSchedulable = new PausingSchedulable(engine, period, type);
	}

	/**
	 * Wakes up the protocol runner.
	 */
	public void wakeUp() {
		fSchedulable.resume();
	}

	/**
	 * Handler which pairs with the PausingSchedulable.
	 */
	@Override
	public void eventPerformed(ISimulationEngine state,
			Schedulable schedulable, double nextShift) {
		super.eventPerformed(state, schedulable, nextShift);
		update(state.network());
	}

	private void update(INetwork net) {
		int active = 0;
		for (int i = 0; i < net.size(); i++) {
			if (((ICyclicProtocol) net.process(i).getProtocol(fPid)).getState() == State.ACTIVE) {
				active++;
			}
		}

		if (active == 0) {
			/**
			 * Stops scheduling the cyclic protocol. It will only be scheduled
			 * again once our IEventObserver is triggered, i.e, when some node
			 * comes up in the network.
			 */
			fSchedulable.pause();
		}
	}

	/**
	 * @return an {@link IEventObserver} which will wake up the protocol runner
	 *         automatically whenever a process logs into the underlying
	 *         network.
	 */
	public IEventObserver networkObserver() {

		/**
		 * This IEventObserver pairs with IProcess.
		 */
		return new IEventObserver() {

			@Override
			public void eventPerformed(ISimulationEngine state,
					Schedulable schedulable, double nextShift) {
				IProcess process = (IProcess) schedulable;
				if (!process.isUp()) {
					return;
				}
				wakeUp();
			}

			@Override
			public boolean isDone() {
				return false;
			}

		};
	}

	class PausingSchedulable extends CyclicSchedulable {

		private boolean fPaused = true;

		private ISimulationEngine fEngine;

		public PausingSchedulable(ISimulationEngine engine, double period,
				int type) {
			super(period, type);
			fEngine = engine;
		}

		@Override
		public boolean isExpired() {
			return fPaused;
		}

		public void pause() {
			fPaused = true;
		}

		public void resume() {
			if (!fPaused) {
				return;
			}
			fPaused = false;

			// Fix to roundoff error when the division is exact.
			IClockData clock = fEngine.clock();
			double currentTime = clock.rawTime();
			setTime(Math.max(currentTime, Math.ceil(clock.rawTime() / fPeriod)
					* fPeriod));
			fEngine.schedule(this);
		}

		public String toString() {
			return "Protocol cycle at " + time();
		}
	}
}
