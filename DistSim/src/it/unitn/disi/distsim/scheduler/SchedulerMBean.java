package it.unitn.disi.distsim.scheduler;

import it.unitn.disi.distsim.control.ServiceMBean;

import java.io.File;

/**
 * JMX configuration management interface for {@link IScheduler}.
 * 
 * @author giuliano
 */
public interface SchedulerMBean extends ServiceMBean {

	public void setSchedulerType(String type);

	public String getSchedulerType();

	public void setSchedulerProperties(String properties);

	public String getSchedulerProperties();

	public File getReplayLog();
	
}
