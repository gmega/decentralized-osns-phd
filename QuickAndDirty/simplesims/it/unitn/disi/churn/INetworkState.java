package it.unitn.disi.churn;

import it.unitn.disi.simulator.core.INetwork;

public interface INetworkState {
	public boolean holds(INetwork network);
}
