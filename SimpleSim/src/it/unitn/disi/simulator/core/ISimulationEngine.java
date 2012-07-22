package it.unitn.disi.simulator.core;

/**
 * Entry point to the set of interfaces which allow simulation participants to
 * query the engine for information regarding its state, as well as call other
 * API events.
 * 
 * @author giuliano
 */
public interface ISimulationEngine {

	public INetwork network();

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
	 * Stops the simulation cold. Useful when one participant detains enough
	 * knowledge to decide it.
	 */
	public void stop();

}
