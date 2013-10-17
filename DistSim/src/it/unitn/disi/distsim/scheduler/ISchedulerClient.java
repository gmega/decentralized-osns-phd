package it.unitn.disi.distsim.scheduler;

import it.unitn.disi.distsim.scheduler.generators.IScheduleIterator;

/**
 * {@link ISchedulerClient} is the interface exported by the scheduler clients
 * that get instantiated by users of the scheduling service.
 * 
 * @author giuliano
 */
public interface ISchedulerClient {

	/**
	 * @return the {@link IScheduleIterator} defining the experiment schedule
	 *         for this client.
	 */
	public abstract IScheduleIterator iterator();

	/**
	 * @return an approximate (initial) size of the schedule.
	 */
	public abstract int size();

}