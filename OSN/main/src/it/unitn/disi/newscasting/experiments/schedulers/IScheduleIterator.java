package it.unitn.disi.newscasting.experiments.schedulers;

/**
 * An {@link IScheduleIterator} provides an iterator interface to experiment
 * schedules. Schedules are nothing more than sequence of {@link Integer} IDs.
 * 
 * @author giuliano
 */
public interface IScheduleIterator {

	/**
	 * Special value returned by {@link #next()}
	 */
	public static final Object DONE = Integer.MAX_VALUE;

	/**
	 * @return the number of remaining IDs in the schedule, or
	 *         {@link ISchedule#UNKNOWN} if this quantity is not known.
	 */
	public int remaining();

	/**
	 * @return the next {@link Integer} in the ID stream, if available, or
	 *         {@link #DONE} if none is available.
	 */
	public Object nextIfAvailable();
}
