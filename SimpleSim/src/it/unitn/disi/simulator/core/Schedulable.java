package it.unitn.disi.simulator.core;

import java.util.ArrayList;

/**
 * Abstract class for something that can be scheduled by the simulator.
 * 
 * @author giuliano
 */
public abstract class Schedulable implements Comparable<Schedulable> {

	private final ArrayList<IEventObserver> fObservers;

	public Schedulable() {
		fObservers = new ArrayList<IEventObserver>();
	}

	/**
	 * An expired {@link Schedulable} will not be re-scheduled by the simulator
	 * once its time expires.
	 * 
	 * @return <code>true</code> if the {@link Schedulable} is to be
	 *         rescheduled, or <code>false</code> otherwise.
	 */
	public abstract boolean isExpired();

	/**
	 * Method called by the scheduler when this object gets scheduled.
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

	/**
	 * Subscribes an observer to the state changes of this process. <BR>
	 * <BR>
	 * The main caveat of installing an observer directly into an
	 * {@link Schedulable} is that its {@link IEventObserver#isBinding()} method
	 * <b>will not</b> be honored. {@link IEventObserver}s installed in
	 * {@link Schedulable}s are, therefore, always non-binding.
	 */
	public void addObserver(IEventObserver observer) {
		fObservers.add(observer);
	}

	protected void notifyObservers(ISimulationEngine state, double next) {
		for (IEventObserver observer : fObservers) {
			observer.eventPerformed(state, this, next);
		}
	}

	@Override
	public final int compareTo(Schedulable other) {
		return (int) Math.signum(this.time() - other.time());
	}
}
