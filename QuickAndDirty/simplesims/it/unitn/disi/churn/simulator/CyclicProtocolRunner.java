package it.unitn.disi.churn.simulator;

import it.unitn.disi.churn.simulator.ICyclicProtocol.State;

public class CyclicProtocolRunner<K extends ICyclicProtocol> implements
		IEventObserver {

	protected final int fPid;

	private boolean fDone;

	private SimpleEDSim fParent;

	public CyclicProtocolRunner(int pid) {
		fPid = pid;
	}

	@Override
	public void simulationStarted(SimpleEDSim parent) {
		fParent = parent;
	}

	@Override
	public void stateShifted(INetwork parent, double time,
			Schedulable schedulable) {

		int done = 0;
		for (int i = 0; i < parent.size(); i++) {
			IProcess process = parent.process(i);
			ICyclicProtocol protocol = (ICyclicProtocol) process
					.getProtocol(fPid);
			State state = protocol.getState();
			if (state != State.DONE) {
				protocol.nextCycle(time, parent, process);
			} else {
				done++;
			}
		}

		if (done == parent.size()) {
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
