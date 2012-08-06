package it.unitn.disi.distsim.scheduler;

import java.rmi.Remote;
import java.rmi.RemoteException;

import it.unitn.disi.utils.collections.Pair;

public interface ISchedulerAdmin extends Remote {

	/**
	 * @return returns a loose snapshot of the currently active workers. Note
	 *         the snapshot is taken without freezing any state, and therefore
	 *         new workers might be missing, or old workers might be included.
	 */
	public Pair<String, Integer>[] registeredWorkers() throws RemoteException;

	/**
	 * @return the total number of experiments.
	 */
	public int total() throws RemoteException;

	/**
	 * @return the percentage of completion for this experiment queue.
	 */
	public double completion() throws RemoteException;

}
