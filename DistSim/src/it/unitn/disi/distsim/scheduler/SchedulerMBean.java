package it.unitn.disi.distsim.scheduler;

import java.io.File;

import it.unitn.disi.distsim.control.ServiceMBean;

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

	public String getQueueName();

	public File getReplayLog();
	
}
