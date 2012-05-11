package it.unitn.disi.churn.simulator;

import it.unitn.disi.churn.simulator.ICyclicProtocol.State;

public class CyclicProtocolRunner<K extends ICyclicProtocol> implements
		IEventObserver {

	protected K[] fProtocol;

	private boolean fDone;

	private SimpleEDSim fParent;

	public CyclicProtocolRunner(K[] protocols) {
		fProtocol = protocols;
	}

	public K get(int idx) {
		return fProtocol[idx];
	}

	@Override
	public void simulationStarted(SimpleEDSim parent) {
		fParent = parent;
	}

	@Override
	public void stateShifted(INetwork parent, double time,
			Schedulable schedulable) {

		int done = 0;
		for (int i = 0; i < fProtocol.length; i++) {
			State state = fProtocol[i].getState();
			if (state != State.DONE) {
				fProtocol[i].nextCycle(time, parent, this);
			} else {
				done++;
			}
		}

		if (done == fProtocol.length) {
			fDone = true;
			fParent.done(this);
		}
	}

	@Override
	public boolean isDone() {
		return fDone;
	}

	@Override
	public boolean isBinding() {
		return true;
	}

	protected SimpleEDSim parent() {
		return fParent;
	}
}
