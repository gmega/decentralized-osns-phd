package scheduler;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface IDaemon extends Remote {
	
	public static final String STANDARD = "standard";
	
	public void start() throws RemoteException;
	
	public void submit(CommandDescriptor command) throws RemoteException;
	
	public void kill(int pid) throws RemoteException;
	
	public void killall() throws RemoteException;
	
	public List<ProcessDescriptor> list() throws RemoteException;

	public void shutdown() throws RemoteException;
	
}
