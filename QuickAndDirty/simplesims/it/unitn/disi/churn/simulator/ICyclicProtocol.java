package it.unitn.disi.churn.simulator;

public interface ICyclicProtocol {

	public void nextCycle(double time, INetwork sim,
			CyclicProtocolRunner<? extends ICyclicProtocol> protocols);

	public boolean isDone();

}
