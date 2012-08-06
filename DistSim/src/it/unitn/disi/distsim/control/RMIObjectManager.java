package it.unitn.disi.distsim.control;

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;

public class RMIObjectManager implements ServiceMBean {

	private static final Logger fLogger = Logger
			.getLogger(RMIObjectManager.class);

	private final boolean fReuse;

	private final int fPort;

	private final Map<String, Remote> fKeys = new HashMap<String, Remote>();

	private boolean fRunning;

	private Thread fShutdown = new Thread() {
		@Override
		public void run() {
			RMIObjectManager.this.stop();
		}
	};

	private Registry fRegObject;

	private Registry fRegStub;

	public RMIObjectManager(int port, boolean reuse) {
		fPort = port;
		fReuse = reuse;
	}

	public synchronized void publish(Remote object, String key)
			throws RemoteException {
		checkRunning();

		try {
			UnicastRemoteObject.exportObject(object, 0);
			fRegStub.rebind(key, object);
		} catch (RemoteException ex) {
			remove(key);
			throw ex;
		}
	}

	public synchronized void remove(String key) {
		checkRunning();

		try {
			fRegStub.unbind(key);
		} catch (NotBoundException ex) {
			// Don't care, could happen.
		} catch (Exception ex) {
			// Failed.
			fLogger.error("Failed to unbind object.", ex);
		}

		Remote object = fKeys.get(key);
		unexport(object);
	}

	public void unexport(Remote object) {
		try {
			if (object != null) {
				UnicastRemoteObject.unexportObject(object, true);
			}
		} catch (RemoteException ex) {
			// Also don't care.
		}
	}

	@Override
	public synchronized void start() {
		if (fRunning) {
			return;
		}

		fLogger.info("Starting registry.");
		try {
			if (!fReuse) {
				fRegObject = LocateRegistry.createRegistry(fPort);
			}
			fRegStub = LocateRegistry.getRegistry(fPort);
		} catch (RemoteException ex) {
			fLogger.error("Error while publishing object.", ex);
			System.exit(-1);
		}
		Runtime.getRuntime().addShutdownHook(fShutdown);
		fRunning = true;
		fLogger.info("All good.");
	}

	@Override
	public synchronized void stop() {
		checkRunning();
		Runtime.getRuntime().removeShutdownHook(fShutdown);

		Iterator<String> it = fKeys.keySet().iterator();
		while (it.hasNext()) {
			remove(it.next());
			it.remove();
		}

		unexport(fRegObject);
		fRunning = false;
	}

	private void checkRunning() {
		if (!isRunning()) {
			throw new IllegalStateException("Server is not running.");
		}
	}

	@Override
	public synchronized boolean isRunning() {
		return fRunning;
	}

}
