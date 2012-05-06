package it.unitn.disi.churn.simulator;

public interface ICyclicProtocol {
	
	public void nextCycle(double time, SimpleEDSim sim,
			CyclicProtocolRunner protocols);
	
	public boolean isDone();
	
}
