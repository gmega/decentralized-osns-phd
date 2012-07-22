package it.unitn.disi.simulator.core;

/**
 * An {@link IEventObserver} is an observer for simulation events.
 * 
 * @author giuliano
 */
public interface IEventObserver {

	public static final Double UNKNOWN = -1D;

	public static final Double EXPIRED = -2D;

	/**
	 * Called when an event occurs (i.e., a {@link Schedulable} gets scheduled).
	 * 
	 * @param engine
	 *            a reference to the simulation engine.
	 * @param schedulable
	 *            the {@link Schedulable} that got scheduled.
	 * @param nextShift
	 *            the next time instant, in raw time, at which this
	 *            {@link Schedulable} will be scheduled. If this is not known,
	 *            then this will be assigned {@link #UNKNOWN}. If
	 *            {@link Schedulable#isExpired()} is true, this will be assigned
	 *            {@link #EXPIRED}.
	 */
	public void eventPerformed(ISimulationEngine engine, Schedulable schedulable,
			double nextShift);

	/**
	 * @return whether this observer is done observing the underlying process or
	 *         not.
	 */
	public boolean isDone();

}
