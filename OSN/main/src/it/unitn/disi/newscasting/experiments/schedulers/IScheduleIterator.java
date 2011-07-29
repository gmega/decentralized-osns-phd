package it.unitn.disi.newscasting.experiments.schedulers;

import java.util.Iterator;

/**
 * Extension of the {@link Iterator} interface for scheduling nodes for
 * experiments (usually unit experiments).
 * 
 * @author giuliano
 */
public interface IScheduleIterator extends Iterator<Integer> {

	/**
	 * Special value returned by {@link #next()} 
	 */
	public static final int NONE_AVAILABLE = Integer.MAX_VALUE;

	public int remaining();
}
