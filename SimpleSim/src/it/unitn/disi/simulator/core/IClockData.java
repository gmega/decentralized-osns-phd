package it.unitn.disi.simulator.core;

/**
 * Interface for querying the simulation engine about time information.
 * 
 * @author giuliano
 */
public interface IClockData {

	/**
	 * @return the current simulation time.
	 */
	public double time();

	/**
	 * @return the current simulation time including the burn-in period.
	 */
	public double rawTime();

	/**
	 * @return <code>true</code> if the simulation is still under burn-in, or
	 *         <code>false</code> otherwise.
	 */
	public boolean isBurningIn();

}
