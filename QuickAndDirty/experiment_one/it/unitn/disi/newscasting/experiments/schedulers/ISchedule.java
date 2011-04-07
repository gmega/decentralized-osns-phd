package it.unitn.disi.newscasting.experiments.schedulers;

/**
 * An {@link ISchedule} represents an {@link Iterable} collection of
 * {@link Integer}s. The only static aspect required of {@link ISchedule}s is
 * that their size must be known in advance.
 * 
 * @author giuliano
 */
public interface ISchedule extends Iterable<Integer> {
	/**
	 * @return the size of this schedule.
	 */
	public int size();
}
