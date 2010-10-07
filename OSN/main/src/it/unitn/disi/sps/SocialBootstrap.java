package it.unitn.disi.sps;

import it.unitn.disi.IRebootable;
import it.unitn.disi.graph.GraphProtocol;
import it.unitn.disi.utils.peersim.INodeRegistry;
import it.unitn.disi.utils.peersim.NodeRegistry;
import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Linkable;
import peersim.core.Network;
import peersim.core.Node;
import peersim.dynamics.NodeInitializer;

/**
 * Bootstraps a node by pointing it to its friends. Can act both as a node
 * initializer (for bootstrapping nodes in dynamic networks) and a network
 * initializer (for bootstrapping the whole network).
 */
public class SocialBootstrap implements NodeInitializer, Control {

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
	 * The linkable with the social network.
	 * 
	 * @config
	 */
	private static final String PAR_SN = "socialNetwork";

	/**
	 * The size of the bootsrap sample.
	 * 
	 * @config
	 */
	private static final String PAR_BOOTSTRAP_SIZE = "bootstrapSize";

	/**
	 * Node degree above which no bootstrapping is required.
	 * 
	 * @config
	 */
	private static final String PAR_MAX_DEGREE = "maxDegree";
	
	private static final String PAR_ADD_INVERSE = "pushpull";

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

	private final int fPid;

	private final int fSnPid;

	private final int fBoostrapSize;

	private final int fMaxDegree;
	
	private final boolean fPushPull;
	
	private final boolean fPack;

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
	public SocialBootstrap(String prefix) {
		fPid = Configuration.getPid(prefix + "." + PAR_PROT);
		fSnPid = Configuration.getPid(prefix + "." + PAR_SN);
		fPack = Configuration.contains(prefix + "." + PAR_PACK);
		fBoostrapSize = Configuration.getInt(prefix + "." + PAR_BOOTSTRAP_SIZE,
				-1);
		fPushPull = Configuration.contains(prefix + "." + PAR_ADD_INVERSE);
		fMaxDegree = Configuration.getInt(prefix + "." + PAR_MAX_DEGREE,
				Integer.MAX_VALUE);
	}

	// --------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------

	/**
	 * Bootstrap a node to one of its friends, if such node exists, or to
	 * nothing, otherwise.
	 */
	public void initialize(Node n) {
		Linkable sps = (Linkable) n.getProtocol(fPid);
		if (sps instanceof IRebootable) {
			((IRebootable)sps).reset();
		}
		
		Linkable sn = (Linkable) n.getProtocol(fSnPid);

		int bSize = fBoostrapSize;
		int degree = sps.degree();

		for (int i = 0; i < sn.degree(); i++) {

			if (bSize == 0 || degree == fMaxDegree) {
				break;
			}
			Node peer = sn.getNeighbor(i);
			sps.addNeighbor(peer);
			bSize--;
			degree++;

			// Adds the inverse link.
			if (fPushPull) {
				Linkable peerSps = (Linkable) peer.getProtocol(fPid);
				if (peerSps.degree() < fMaxDegree) {
					peerSps.addNeighbor(n);
				}
			}
		}
		
		assert sps.degree() <= fMaxDegree;

		if (fPack) {
			sps.pack();
		}
	}

	public boolean execute() {
		for (int i = 0; i < Network.size(); i++) {
			initialize(Network.get(i));
		}

		return true;
	}

}
