package it.unitn.disi.churn.simulator;

public class CyclicProtocolRunner implements IEventObserver {

	private ICyclicProtocol[] fProtocol;
	
	private int fDone;
		
	public CyclicProtocolRunner(ICyclicProtocol[] protocols) {
		fProtocol = protocols;
	}

	public ICyclicProtocol get(int idx) {
		return fProtocol[idx];
	}

	@Override
	public void simulationStarted(SimpleEDSim parent) {
	}

	@Override
	public void stateShifted(SimpleEDSim parent, double time,
			Schedulable schedulable) {
		
		for (int i = 0; i < fProtocol.length; i++) {
			if (!fProtocol[i].isDone()) {
				fProtocol[i].nextCycle(time, parent, this);
				if (fProtocol[i].isDone()) {
					fDone++;
				}
			}
		}
	}

	@Override
	public boolean isDone() {
		return fDone == fProtocol.length;
	}

}
