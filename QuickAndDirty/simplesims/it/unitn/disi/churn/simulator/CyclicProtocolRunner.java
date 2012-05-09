package it.unitn.disi.churn.simulator;

public class CyclicProtocolRunner<K extends ICyclicProtocol> implements IEventObserver {

	private K[] fProtocol;
	
	private boolean fDone;
	
	private SimpleEDSim fParent;
		
	public CyclicProtocolRunner(K [] protocols) {
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
			if (!fProtocol[i].isDone()) {
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

}
