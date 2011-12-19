package it.unitn.disi.churn;

import peersim.util.IncrementalStats;
import it.unitn.disi.churn.RenewalProcess.State;

public interface IChurnSim {

	public void simulationStarted(BaseChurnSim parent, Object stats);

	public void stateShifted(BaseChurnSim parent, double time,
			RenewalProcess process, State old, State nw);

	public boolean isDone();
	
	public void printStats(Object stats);
	
}
