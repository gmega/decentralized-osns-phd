package it.unitn.disi.distsim.control;

import java.rmi.Remote;
import java.util.Properties;

public interface IStatusListener extends Remote {

	/**
	 * Notifies subscribed clients of a change of values in a set of properties.
	 * 
	 * @param delta
	 *            the {@link Properties} object containing the changed values.
	 */
	public void propertyChanged(Properties delta);

}
