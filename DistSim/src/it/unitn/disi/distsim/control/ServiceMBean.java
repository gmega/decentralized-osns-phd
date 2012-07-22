package it.unitn.disi.distsim.control;

/**
 * Management interface for a simulation service.
 * 
 * @author giuliano
 */
public interface ServiceMBean {

	/**
	 * Starts a service.
	 * 
	 * @throws IllegalStateException
	 *             if the service is already running.
	 */
	public void start();

	/**
	 * Stops a service.
	 * 
	 * @throws IllegalStateException
	 *             if the service is not running.
	 */
	public void stop();

	/**
	 * @return <code>true</code> if the service is running, or
	 *         <code>false</code> otherwise.
	 */
	public boolean isRunning();

}
