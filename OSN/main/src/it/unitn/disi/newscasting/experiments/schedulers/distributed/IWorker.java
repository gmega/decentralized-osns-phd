package it.unitn.disi.newscasting.experiments.schedulers.distributed;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IWorker extends Remote {
	
	public void echo() throws RemoteException;
	
}
