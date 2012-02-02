package it.unitn.disi.churn;

import it.unitn.disi.churn.RenewalProcess.State;

/**
 * An {@link IChurnSim} is an observer for a network under churn. 
 * 
 * @author giuliano
 */
public interface IChurnSim {

	/**
	 * Called when the simulation starts.
	 * 
	 * @param parent
	 * @param stats
	 */
	public void simulationStarted(BaseChurnSim parent);

	/**
	 * Called when a node changes state.
	 * 
	 * @param parent
	 * @param time
	 * @param process
	 * @param old
	 * @param nw
	 */
	public void stateShifted(BaseChurnSim parent, double time,
			RenewalProcess process, State old, State nw);

	/**
	 * @return whether this observer is done observing the underlying
	 * process or not.
	 */
	public boolean isDone();
}
