package it.unitn.disi.distsim.streamserver;

import it.unitn.disi.distsim.control.ManagedService;
import it.unitn.disi.distsim.control.ServiceMBean;

import java.io.File;

public interface StreamServerMBean extends ServiceMBean, ManagedService {

	public int getPort();
	
	public void setPort(int port);

	public File getOutputFolder();
	
}
