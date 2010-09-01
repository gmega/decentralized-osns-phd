package it.unitn.disi.test.framework;

import peersim.config.Configuration;
import peersim.core.Linkable;
import peersim.core.Network;
import peersim.core.Node;
import peersim.dynamics.NodeInitializer;

/**
 * Bootstraps a node by pointing it always to the same node.
 */
public class NodeZeroNI implements NodeInitializer {

	// --------------------------------------------------------------------------
	// Parameters
	// --------------------------------------------------------------------------

	/**
	 * The protocol to operate on.
	 * 
	 * @config
	 */
	private static final String PAR_PROT = "protocol";

	/**
	 * If this config property is defined, method {@link Linkable#pack()} is
	 * invoked on the specified protocol at the end of the wiring phase. Default
	 * to false.
	 * 
	 * @config
	 */
	private static final String PAR_PACK = "pack";

	// --------------------------------------------------------------------------
	// Fields
	// --------------------------------------------------------------------------

	/**
	 * The protocol we want to wire
	 */
	private final int pid;

	/**
	 * If true, method pack() is invoked on the initialized protocol
	 */
	private final boolean pack;

	/**
	 *  A cached reference to the node with ID 0 -- the node to which every node
	 *  will point to when being initialized.
	 */
	private Node zero;

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
	public NodeZeroNI(String prefix) {
		pid = Configuration.getPid(prefix + "." + PAR_PROT);
		pack = Configuration.contains(prefix + "." + PAR_PACK);
	}

	// --------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------

	/**
	 * Takes {@value #PAR_DEGREE} random samples with replacement from the nodes
	 * of the overlay network. No loop edges are added.
	 */
	public void initialize(Node n) {
		if (Network.size() == 0)
			return;

		Node zero = nodeZero();
		Linkable linkable = (Linkable) n.getProtocol(pid);
		linkable.addNeighbor(zero);

		if (pack)
			linkable.pack();
	}
	

	private Node nodeZero() {
		if (zero == null) {
			for (int i = 0; i < Network.size(); i++) {
				Node candidate = Network.get(i);
				if (candidate.getID() == 0) {
					zero = candidate;
					break;
				}
			}
			
			if (zero == null) {
				throw new RuntimeException("Node zero could not be found.");
			}
		}
		
		return zero;
	}

}
