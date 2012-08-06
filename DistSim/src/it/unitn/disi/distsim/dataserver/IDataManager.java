package it.unitn.disi.distsim.dataserver;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IDataManager extends Remote {

	/**
	 * Returns work unit data to a requesting worker.
	 * 
	 * @param expId
	 *            the work unit id.
	 * @param wtype
	 *            the worker type. This is useful when different configurations
	 *            apply to different workers.
	 * 
	 * @return a {@link WorkUnit} object, with the configuration (if available)
	 *         and existing checkpoint (if available).
	 */
	public WorkUnit workUnit(int wid, String wtype) throws RemoteException;

	public void writeCheckpoint(int wid, byte[] state) throws RemoteException;

}
