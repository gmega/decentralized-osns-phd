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
	 * Deletes a simulation with a given ID, and clears all of its files.
	 * 
	 * @param id
	 *            the unique id of this simulation.
	 */
	public void delete(String id);

	/**
	 * Resets all services registered under a simulation.
	 * 
	 * @param id
	 *            the unique id of the simulation to reset.
	 */
	public void reset(String id);

	/**
	 * @return the master output folder (at the server) in which all simulation
	 *         data will be stored.
	 */
	public File getMasterFolder();

}
