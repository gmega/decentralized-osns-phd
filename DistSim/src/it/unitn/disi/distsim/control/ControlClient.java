package it.unitn.disi.distsim.control;

import it.unitn.disi.utils.MiscUtils;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import org.apache.log4j.Logger;

import peersim.config.Attribute;
import peersim.config.AutoConfig;

/**
 * Client-side component for accessing simulation controls.
 * 
 * @author giuliano
 */
@AutoConfig
public class ControlClient {

	private static final Logger fLogger = Logger.getLogger(ControlClient.class);

	private final String fControllerHost;

	private final int fControlPort;

	private final String fSimId;

	private Registry fRegistry;

	public ControlClient(@Attribute("control.host") String host,
			@Attribute("control.port") int port,
			@Attribute("sim-id") String simId) {
		fControllerHost = host;
		fControlPort = port;
		fSimId = simId;
	}

	@SuppressWarnings("unchecked")
	public <T> T lookup(String serviceKey, Class<T> cls) throws RemoteException {
		Registry rmiReg = registry();
		String key = SimulationControl.name(fSimId, serviceKey);
		try {
			return (T) rmiReg.lookup(key);
		} catch (NotBoundException ex) {
			fLogger.error("Service not bound under expected registry location "
					+ key + ".");
			throw MiscUtils.nestRuntimeException(ex);
		}
	}

	private Registry registry() throws RemoteException {
		if (fRegistry == null) {
			fRegistry = LocateRegistry.getRegistry(fControllerHost,
					fControlPort);
		}

		return fRegistry;
	}

}
