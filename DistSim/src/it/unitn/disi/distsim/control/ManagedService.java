package it.unitn.disi.distsim.control;

/**
 * Interface implemented by all {@link ServiceMBean}s that are managed by
 * {@link SimulationControl}.
 * 
 * @author giuliano
 */
public interface ManagedService extends ServiceMBean {

	/**
	 * @return <code>true</code> if this service should auto-start, or
	 *         <code>false</code> otherwise.
	 */
	public boolean shouldAutoStart();

	/**
	 * Sets the {@link ISimulation} connected to this service.
	 */
	public void setSimulation(ISimulation parent);

}
