package it.unitn.disi.churn.simulator;

public interface INetwork {
	
	public int size();

	public IProcess process(int index);

}