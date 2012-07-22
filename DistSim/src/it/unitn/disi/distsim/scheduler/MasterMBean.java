package it.unitn.disi.distsim.scheduler;

import it.unitn.disi.distsim.control.ServiceMBean;

/**
 * JMX configuration management interface for {@link IMaster}.
 * 
 * @author giuliano
 */
public interface MasterMBean extends ServiceMBean {

	public void setSchedulerType(String type);

	public String getSchedulerType();

	public void setSchedulerProperties(String properties);

	public String getSchedulerProperties();

	public String getQueueName();

	public String getReplayLog();
	
}
