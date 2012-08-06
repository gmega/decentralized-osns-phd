package it.unitn.disi.distsim.control;

import java.io.File;

/**
 * Main interface for creating/destroying simulations.
 * 
 * @author giuliano
 */
public interface SimulationControlMBean {

	/**
	 * Creates a new simulation with a given id.
	 * 
	 * @param id
	 *            the unique id of this simulation.
	 */
	public void create(String id);

	/**
	 * @return the master output folder (at the server) in which all simulation
	 *         data will be stored.
	 */
	public File getMasterOutputFolder();

	/**
	 * @return the config folder from which configuration files are to be read
	 *         and written.
	 */
	public File getConfigFolder();

}
