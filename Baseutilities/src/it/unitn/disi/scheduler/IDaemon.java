package it.unitn.disi.scheduler;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface IDaemon extends Remote {
		
	public void start() throws RemoteException;
	
	public void submit(CommandDescriptor command) throws RemoteException;
	
	public List<ProcessDescriptor> list() throws RemoteException;
	
	public void kill(int pid) throws RemoteException;
	
	public void killall() throws RemoteException;
	
	/**
	 * Shuts down the server. This method:
	 * <ol>
	 * <li>kills all processes started by this server that are currently
	 * running;</li>
	 * <li>ensures that processes queued at the time this method was called
	 * won't be scheduled after it returns.</li>
	 * </ol>
	 * 
	 * Moreover, this method won't return until all processes are dead, and all
	 * internal threads are known to be stopped.
	 */
	public void shutdown() throws RemoteException;
	
}
