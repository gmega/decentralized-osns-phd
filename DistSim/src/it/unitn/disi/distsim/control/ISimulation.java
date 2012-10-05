package it.unitn.disi.distsim.control;

import java.io.File;

public interface ISimulation extends IObjectRegistry {
	
	public String id();

	public File baseFolder();
	
	public void attributeListUpdated(ManagedService service);
	
}
