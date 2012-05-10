package it.unitn.disi.churn.simulator;

import it.unitn.disi.churn.simulator.ICyclicProtocol.State;

public class CyclicProtocolRunner<K extends ICyclicProtocol> implements
		IEventObserver {

	private K[] fProtocol;

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
			if (state == State.ACTIVE) {
				fProtocol[i].nextCycle(time, parent, this);
			} else if (state == State.DONE) {
				done++;
			}
			protocolState(i, state);
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

	// -------------------------------------------------------------------------
	// Hooks for subclasses interested in protocol state.
	// -------------------------------------------------------------------------
	
	protected void protocolState(int index, State state) {
		
	}
	
	protected SimpleEDSim parent() {
		return fParent;
	}
}
