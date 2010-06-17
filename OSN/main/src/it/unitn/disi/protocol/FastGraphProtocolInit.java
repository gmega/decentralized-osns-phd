package it.unitn.disi.protocol;

import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;
import peersim.dynamics.NodeInitializer;

public class FastGraphProtocolInit implements NodeInitializer, Control {

	// --------------------------------------------------------------------------
	// Parameters
	// --------------------------------------------------------------------------

	/**
	 * The protocol to operate on.
	 * 
	 * @config
	 */
	private static final String PAR_PROT = "protocol";

	// --------------------------------------------------------------------------
	// Fields
	// --------------------------------------------------------------------------

	/**
	 * The protocol for which we'll be boostrapping.
	 */
	private final int fPid;

	// --------------------------------------------------------------------------
	// Initialization
	// --------------------------------------------------------------------------

	/**
	 * Standard constructor that reads the configuration parameters. Invoked by
	 * the simulation engine.
	 * 
	 * @param prefix
	 *            the configuration prefix for this class
	 */
	public FastGraphProtocolInit(String prefix) {
		fPid = Configuration.getPid(prefix + "." + PAR_PROT);
	}

	// --------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------

	public void initialize(Node n) {
		FastGraphProtocol sn = (FastGraphProtocol) n.getProtocol(fPid);
		INodeRegistry reg = NodeRegistry.getInstance();
		if (!reg.contains(n.getID())) {
			sn.bind(n);
			NodeRegistry.getInstance().registerNode(n);
		}
	}

	public boolean execute() {
		for (int i = 0; i < Network.size(); i++) {
			initialize(Network.get(i));
		}

		return true;
	}

}

