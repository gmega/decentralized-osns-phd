package it.unitn.disi.simulator.protocol;

import it.unitn.disi.simulator.core.IProcess;
import it.unitn.disi.simulator.core.ISimulationEngine;

public interface ICyclicProtocol {
	
	public static enum State {
		IDLE, ACTIVE, WAITING, DONE;
	}

	public void nextCycle(ISimulationEngine state, IProcess process);

	public State getState();

}
