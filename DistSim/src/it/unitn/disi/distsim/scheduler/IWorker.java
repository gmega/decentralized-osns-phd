package it.unitn.disi.distsim.scheduler;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IWorker extends Remote {
	
	public void echo() throws RemoteException;
	
}
