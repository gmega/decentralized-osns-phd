package it.unitn.disi.distsim.streamserver;

import it.unitn.disi.distsim.control.ServiceMBean;

public interface StreamServerMBean extends ServiceMBean{

	public int getPort();
	
	public void setPort(int port);

	public String getOutputFolder();
	
}
