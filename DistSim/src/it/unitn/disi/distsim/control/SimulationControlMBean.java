package it.unitn.disi.distsim.control;

import java.io.File;

public interface SimulationControlMBean {

	public void create(String id);
	
	public File getMasterOutputFolder();
	
}
