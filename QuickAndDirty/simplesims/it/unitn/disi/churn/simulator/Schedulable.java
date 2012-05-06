package it.unitn.disi.churn.simulator;

/**
 * Abstract class for something that can be scheduled by the simulator.
 * 
 * @author giuliano
 */
public abstract class Schedulable implements Comparable<Schedulable> {

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
	 * @param time
	 *            the time at which the schedulable has been scheduled.
	 * @param parent
	 *            the parent scheduler.
	 */
	public abstract void scheduled(double time, SimpleEDSim parent);

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
	public final int compareTo(Schedulable other) {
		return (int) Math.signum(this.time() - other.time());
	}
}
