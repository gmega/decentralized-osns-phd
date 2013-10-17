package it.unitn.disi.distsim.scheduler;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Properties;

public interface IWorker extends Remote {
	
	public void echo() throws RemoteException;
	
	public Properties status() throws RemoteException;
	
	// public void subscribe(IStatusListener listener) throws RemoteException
	
	// public void unsubscribe(IStatusListener listener) throws RemoteException
	
}
