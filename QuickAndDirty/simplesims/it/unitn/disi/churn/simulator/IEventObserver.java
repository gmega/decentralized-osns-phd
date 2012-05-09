package it.unitn.disi.churn.simulator;

/**
 * An {@link IEventObserver} is an observer for a network under churn.
 * 
 * @author giuliano
 */
public interface IEventObserver {

	/**
	 * Called when the simulation starts.
	 * 
	 * @param parent
	 * @param stats
	 */
	public void simulationStarted(SimpleEDSim parent);

	/**
	 * Called when a schedulable gets scheduled.
	 */
	public void stateShifted(INetwork parent, double time,
			Schedulable schedulable);

	/**
	 * @return whether this observer is done observing the underlying process or
	 *         not.
	 */
	public boolean isDone();

	/**
	 * @return when all binding {@link IEventObserver}s are done, the simualtion
	 *         ends.
	 */
	public boolean isBinding();
}
