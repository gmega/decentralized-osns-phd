package it.unitn.disi.simulator.core;

import java.io.Serializable;

/**
 * Abstract class for something that can be scheduled by the simulator. The base
 * class is deprived of any fields so as to keep it as lightweight as possible,
 * delegating the option of adding features to subclasses.
 * 
 * @author giuliano
 */
public abstract class Schedulable implements Comparable<Schedulable>,
		Serializable {

	private static final long serialVersionUID = 1L;
	
	public static final int UNTYPED = -1;

	/**
	 * An expired {@link Schedulable} will not be re-scheduled by the simulator
	 * once its time expires.
	 * 
	 * @return <code>true</code> if the {@link Schedulable} is to be
	 *         rescheduled, or <code>false</code> otherwise.
	 */
	public abstract boolean isExpired();

	/**
	 * Method called by the scheduler when this schedulable gets scheduled.
	 * Schedulables that need to be rescheduled should update their scheduling
	 * time during the execution of this method.
	 * 
	 * @param state
	 *            the current state of the simulation.
	 * @see ISimulationEngine
	 */
	public abstract void scheduled(ISimulationEngine state);

	/**
	 * @return the scheduling time for this schedulable.
	 */
	public abstract double time();

	/**
	 * @return a numeric type for the schedulable. The scheduler will pair
	 *         scheduling events of a schedulable with a certain {@link #type()}
	 *         with {@link IEventObserver} instances registered as listeners to
	 *         that type.
	 * 
	 */
	public abstract int type();

	@Override
	public int compareTo(Schedulable other) {
		return (int) Math.signum(this.time() - other.time());
	}
}
