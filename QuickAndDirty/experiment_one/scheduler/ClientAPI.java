package scheduler;

import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.rmi.server.UnicastRemoteObject;

import org.apache.log4j.Logger;

/**
 * Service interaction API that allows the simple scheduler to be embedded
 * elsewhere.
 * 
 * @author giuliano
 */
public class ClientAPI {
	
	private static final Logger fLogger = Logger.getLogger(ClientAPI.class);

	private final int fPort;

	public ClientAPI(int port) {
		fPort = port;
	}

	public boolean isRunning() throws RemoteException {
		return resolveObject() != null;
	}

	/**
	 * Starts the server, and blocks until it is shut down, or the current
	 * thread is interrupted.
	 * 
	 * @param cores
	 * @throws ExportException
	 * @throws RemoteException
	 */
	public void start(int cores) throws ExportException, RemoteException {
		fLogger.info("RMI registry start.");
		LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
		
		fLogger.info("Publishing RMI reference.");
		Daemon daemon = new Daemon(cores);
		IDaemon stub = (IDaemon) UnicastRemoteObject
				.exportObject(daemon, fPort);
		Registry registry = LocateRegistry.getRegistry();
		registry.rebind(Daemon.SCHEDULER_DAEMON, stub);
		
		fLogger.info("Starting server.");
		daemon.start();
		
		fLogger.info("All good.");
		try {
			daemon.join();
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

	public IDaemon resolveObject() throws RemoteException {
		try {
			Registry registry = LocateRegistry.getRegistry();
			return (IDaemon) registry.lookup(Daemon.SCHEDULER_DAEMON);
		} catch (NotBoundException ex) {
			return null;
		} catch (ConnectException ex) {
			return null;
		}
	}
}
