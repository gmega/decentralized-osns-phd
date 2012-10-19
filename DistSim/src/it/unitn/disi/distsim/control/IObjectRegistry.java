package it.unitn.disi.distsim.control;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface to a simple object registry.
 * 
 * @author giuliano
 */
public interface IObjectRegistry {
	public void publish(String key, Remote object) throws RemoteException;

	public void remove(String key);
}
