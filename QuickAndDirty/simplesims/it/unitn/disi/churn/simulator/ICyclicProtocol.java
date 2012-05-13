package it.unitn.disi.churn.simulator;

public interface ICyclicProtocol {
	
	public static enum State {
		ACTIVE, WAITING, DONE;
	}

	public void nextCycle(double time, INetwork sim, IProcess process);

	public State getState();

}
