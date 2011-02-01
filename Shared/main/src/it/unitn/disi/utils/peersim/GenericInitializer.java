package it.unitn.disi.utils.peersim;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;
import peersim.dynamics.NodeInitializer;

/**
 * Generic initialization control for {@link IInitializable}s.  
 * 
 * @author giuliano
 */
@AutoConfig
public class GenericInitializer implements Control, NodeInitializer {

	private int[] fProtocolIds;

	public GenericInitializer(@Attribute("protocol") String protocolNames) {
		String[] nameArray = protocolNames.split(" ");
		fProtocolIds = new int[nameArray.length];
		for (int i = 0; i < nameArray.length; i++) {
			fProtocolIds[i] = Configuration.lookupPid(nameArray[i]);
		}
	}

	@Override
	public boolean execute() {
		for (int i = 0; i < Network.size(); i++) {
			initialize(Network.get(i));
		}
		return false;
	}

	@Override
	public void initialize(Node n) {
		for (int pid : fProtocolIds) {
			IInitializable initializable = (IInitializable) n.getProtocol(pid);
			initializable.initialize(n);
		}
	}

}
