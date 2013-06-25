package it.unitn.disi.distsim.scheduler;

import it.unitn.disi.utils.collections.Pair;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface IScheduler extends Remote {
	
	public static final String MASTER_ADDRESS = IScheduler.class.getName();

	/**
	 * Registers a new worker.
	 * 
	 * @param worker
	 *            the worker being registered.
	 * 
	 * @return a worker ID.
	 */
	public int registerWorker(IWorker worker) throws RemoteException;

	/**
	 * Asks for a new experiment, acquiring the obligation to run it.
	 * 
	 * @return a {@link Pair} containing the experiment ID and the number of
	 *         remaining experiments (estimate).
	 */
	public Pair<Integer, Integer> acquireExperiment(int workerId)
			throws RemoteException;

	/**
	 * Method allowing clients to retrieve the number of remaining experiments
	 * without having to acquire one. 
	 * 
	 * @return the number of remaining experiments (estimate).
	 */
	public int remaining() throws RemoteException;

	/**
	 * Called back by the worker once the experiment he last acquired runs
	 * successfully.
	 * 
	 * @param experimentId
	 *            ID of the experiment that has been run.
	 */
	public void experimentDone(Integer experimentId) throws RemoteException;
	
	/**
	 * @return a list of registered {@link IWorker}s
	 */
	public List<IWorker> workerList() throws RemoteException;

}
