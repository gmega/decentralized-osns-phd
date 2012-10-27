package it.unitn.disi.distsim.scheduler;

import it.unitn.disi.distsim.control.ResettableService;
import it.unitn.disi.distsim.scheduler.generators.Schedulers;

import java.io.File;

/**
 * JMX configuration management interface for {@link IScheduler}.
 * 
 * @author giuliano
 */
public interface SchedulerMBean extends ResettableService {

	/**
	 * @return the total number of jobs in this schedule.
	 */
	public int getTotal();

	/**
	 * @return the number of jobs still remaining in the schedule.
	 */
	public int getRemaining();

	/**
	 * @return the number of jobs currently assigned to workers.
	 */
	public int getAssigned();

	/**
	 * @return the total number of registered workers.
	 */
	public int getRegisteredWorkers();

	/**
	 * @return the {@link File} containing the log of which jobs have been
	 *         completed, and which haven't.
	 */
	public File getReplayLog();

	/**
	 * @param type
	 *            one of {@link Schedulers.SchedulerType}.
	 */
	public void setSchedulerType(String type);

	/**
	 * @return the currently configured scheduler type, or the string
	 *         <code>"none"</code> if none is configured.
	 */
	public String getSchedulerType();

	/**
	 * @param properties
	 *            sets the configuration properties of this scheduler. Those are
	 *            scheduler-dependant. Properties should be in the format:
	 * 
	 *            <code>key1=value1:key2=value2:key3=value3 ...</code>
	 */
	public void setSchedulerProperties(String properties);

	/**
	 * @return the previously set configuration properties, or the empty string
	 *         "" if no property have been set.
	 */
	public String getSchedulerProperties();

}
