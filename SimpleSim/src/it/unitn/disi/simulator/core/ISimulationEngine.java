package it.unitn.disi.simulator.core;

/**
 * Entry point to the set of interfaces which allow simulation participants to
 * query the engine for information regarding its state, as well as call other
 * API events.
 * 
 * @author giuliano
 */
public interface ISimulationEngine {

	/**
	 * @see INetwork
	 */
	public INetwork network();

	/**
	 * @see IClockData
	 */
	public IClockData clock();

	/**
	 * Schedules a {@link Schedulable} for simulation.
	 * 
	 * @param schedulable
	 */
	public void schedule(Schedulable schedulable);

	/**
	 * Called by an {@link IEventObserver} when it no longer has any work to do.
	 * 
	 * @param observer
	 */
	public void unbound(IEventObserver observer);

	/**
	 * Decreases the number of stop permits in the current engine by one. When
	 * the count reaches zero, the engine arrests.
	 */
	public void stop();

	/**
	 * @return the number of available stop permits in this engine.
	 */
	public int stopPermits();

	/**
	 * @return <code>true</code> if the simulation is done, or
	 *         <code>false</code> otherwise.
	 */
	public boolean isDone();

}
