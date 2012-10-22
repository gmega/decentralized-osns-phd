package it.unitn.disi.distsim.control;

/**
 * A service that can be resetted. The meaning of resetting is
 * service-dependent, but normally means flushing all persistent state, as if
 * the service had just been instantiated by the first time.
 * 
 * @author giuliano
 */
public interface ResettableService extends ServiceMBean {

	public void reset();

}
