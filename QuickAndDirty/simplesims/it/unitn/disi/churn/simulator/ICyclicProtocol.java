package it.unitn.disi.churn.simulator;

public interface ICyclicProtocol {
	
	public static enum State {
		ACTIVE, WAITING, DONE;
	}

	public void nextCycle(double time, INetwork sim,
			CyclicProtocolRunner<? extends ICyclicProtocol> protocols);

	public State getState();

}
