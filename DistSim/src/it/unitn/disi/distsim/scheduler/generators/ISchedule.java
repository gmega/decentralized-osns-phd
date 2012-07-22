package it.unitn.disi.distsim.scheduler.generators;


/**
 * An {@link ISchedule} represents an iterable collection of {@link Integer}s
 * identifying experiments.
 * 
 * @author giuliano
 */
public interface ISchedule {

	public static final Integer UNKNOWN = Integer.MAX_VALUE;

	/**
	 * @return the initial size of this schedule, or {@link #UNKNOWN} if such
	 *         size cannot be established in advance. Note that due to the
	 *         dynamic nature of some schedules, this value might be only an
	 *         approximation.
	 */
	public int size();

	/**
	 * @return an {@link IScheduleIterator} for this schedule. The meaning of
	 *         calling {@link #iterator()} more than once is
	 *         implementation-dependent.
	 */
	public IScheduleIterator iterator();
}
