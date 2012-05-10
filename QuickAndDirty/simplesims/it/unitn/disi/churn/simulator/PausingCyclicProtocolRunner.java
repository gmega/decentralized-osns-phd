package it.unitn.disi.churn.simulator;

import it.unitn.disi.churn.simulator.ICyclicProtocol.State;

public class PausingCyclicProtocolRunner<K extends ICyclicProtocol> extends
		CyclicProtocolRunner<K> {

	private boolean[] fActive;

	private int fActiveCount = 0;

	private PausingSchedulable fSchedulable;

	public PausingCyclicProtocolRunner(K[] protocols, double period, int type) {
		super(protocols);
		fActive = new boolean[protocols.length];
		fSchedulable = new PausingSchedulable(period, type);
		for (int i = 0; i < protocols.length; i++) {
			protocolState(i, protocols[i].getState());
		}
	}

	@Override
	protected void protocolState(int index, State state) {
		fActiveCount = mark(fActive, index, state == State.ACTIVE, fActiveCount);
		if (fActiveCount == 0) {
			fSchedulable.pause();
		}
	}

	private int mark(boolean[] array, int index, boolean state, int cardinality) {
		if (array[index] != state) {
			cardinality += state ? 1 : -1;
			array[index] = state;
		}
		return cardinality;
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
				fSchedulable.resume(time);
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

		private boolean fPaused;

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

		public void resume(double time) {
			if (!fPaused) {
				return;
			}
			fPaused = false;
			setTime(Math.ceil(time / fPeriod) * fPeriod);
			parent().schedule(this, true);
		}

	}
}
