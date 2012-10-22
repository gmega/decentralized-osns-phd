package it.unitn.disi.distsim.scheduler;

import it.unitn.disi.distsim.control.ResettableService;

import java.io.File;

/**
 * JMX configuration management interface for {@link IScheduler}.
 * 
 * @author giuliano
 */
public interface SchedulerMBean extends ResettableService {

	public void setSchedulerType(String type);
	
	public int total();
	
	public int remaining();

	public String getSchedulerType();

	public void setSchedulerProperties(String properties);

	public String getSchedulerProperties();

	public File getReplayLog();

}
