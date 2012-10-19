package it.unitn.disi.distsim.control;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import org.apache.log4j.Logger;

import peersim.config.Attribute;
import peersim.config.AutoConfig;

/**
 * Client-side component for accessing {@link SimulationControl} and its
 * services.
 * 
 * @author giuliano
 */
@AutoConfig
public class ControlClient {

	private static final Logger fLogger = Logger.getLogger(ControlClient.class);

	private final String fControlHost;

	private final int fControlPort;

	private final String fSimId;

	private Registry fRegistry;

	public ControlClient(@Attribute("control.host") String host,
			@Attribute("control.port") int port,
			@Attribute("sim-id") String simId) {
		fControlHost = host;
		fControlPort = port;
		fSimId = simId;
	}

	/**
	 * Looks up a service.
	 * 
	 * @param serviceKey
	 *            the service key.
	 * @param cls
	 *            the expected interface for the service.
	 * @return a reference to the service, or <code>null</code> if a service
	 *         with the require id cannot be found.
	 * 
	 * @throws RemoteException
	 *             if something goes wrong while contacting the service
	 *             registry.
	 */
	@SuppressWarnings("unchecked")
	public <T> T lookup(String serviceKey, Class<T> cls) throws RemoteException {
		Registry rmiReg = registry();
		String key = SimulationControl.name(fSimId, serviceKey);
		try {
			return (T) rmiReg.lookup(key);
		} catch (NotBoundException ex) {
			fLogger.error("Service not bound under expected registry location "
					+ key + ".");
			return null;
		}
	}

	private Registry registry() throws RemoteException {
		if (fRegistry == null) {
			fRegistry = LocateRegistry.getRegistry(fControlHost, fControlPort);
		}

		return fRegistry;
	}

}
