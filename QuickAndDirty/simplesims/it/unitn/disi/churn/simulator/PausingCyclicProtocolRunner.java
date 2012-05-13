package it.unitn.disi.churn.simulator;

import it.unitn.disi.churn.simulator.ICyclicProtocol.State;

/**
 * {@link PausingCyclicProtocolRunner} stops scheduling the cyclic protocol when
 * all nodes go quiescent, until there is a change in the underlying network (a
 * node logs in).
 * 
 * @author giuliano
 * 
 * @param <K>
 */
public class PausingCyclicProtocolRunner<K extends ICyclicProtocol> extends
		CyclicProtocolRunner<K> {

	private PausingSchedulable fSchedulable;

	public PausingCyclicProtocolRunner(double period, int type, int pid) {
		super(pid);
		fSchedulable = new PausingSchedulable(period, type);
	}

	@Override
	public void stateShifted(INetwork parent, double time,
			Schedulable schedulable) {
		super.stateShifted(parent, time, schedulable);
		update(parent);
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

	public IEventObserver networkObserver() {
		return new IEventObserver() {
			@Override
			public void stateShifted(INetwork parent, double time,
					Schedulable schedulable) {
				IProcess process = (IProcess) schedulable;
				if (!process.isUp()) {
					return;
				}
				fSchedulable.resume();
			}

			@Override
			public void simulationStarted(SimpleEDSim parent) {
			}

			@Override
			public boolean isDone() {
				return false;
			}

			@Override
			public boolean isBinding() {
				return false;
			}
		};
	}

	class PausingSchedulable extends CyclicSchedulable {

		private boolean fPaused = true;

		public PausingSchedulable(double period, int type) {
			super(period, type);
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
			setTime(Math.ceil(parent().currentTime() / fPeriod) * fPeriod);
			parent().schedule(this);
		}

		public String toString() {
			return "Protocol cycle at " + time();
		}
	}
}
