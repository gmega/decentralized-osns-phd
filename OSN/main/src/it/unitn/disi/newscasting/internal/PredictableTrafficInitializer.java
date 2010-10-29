package it.unitn.disi.newscasting.internal;

import it.unitn.disi.application.SimpleTrafficGenerator;
import it.unitn.disi.utils.peersim.INodeRegistry;
import it.unitn.disi.utils.peersim.NodeRegistry;

import java.util.Random;

import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;
import peersim.dynamics.NodeInitializer;

/**
 * {@link PredictableTrafficInitializer} allows the application traffic to be
 * generated predictably by drawing the seeds for the nodes in the network from
 * a random generator with a user-specified seed.
 * 
 * This class makes the assumption that the {@link NodeRegistry} has been
 * properly populated, and that {@link Node} IDs occupy a continuous range that
 * goes from zero to a maximum value, which is the network size.
 * 
 * Further, bear in mind that all this class does is maintain a seed-to-node-id
 * mapping. If IDs get mapped to (topologically) different nodes at each
 * simulation run (e.g. the same ID gets mapped to different nodes in a social
 * network) then results might not be as expected.
 * 
 * @author giuliano
 */
public class PredictableTrafficInitializer implements Control, NodeInitializer {

	// ----------------------------------------------------------------------
	// Configuration parameters keys.
	// ----------------------------------------------------------------------

	private static final String PAR_APPLICATION = "application";
	
	private static final String PAR_GENERATOR_SEED = "seed";

	// ----------------------------------------------------------------------
	// Parameter storage.
	// ----------------------------------------------------------------------
	
	private final int fApplicationId;
	
	// ----------------------------------------------------------------------
	// State.
	// ----------------------------------------------------------------------
	
	private final Random fSeedGenerator;
	
	private long fNextExpected;
	
	// ----------------------------------------------------------------------
	
	public PredictableTrafficInitializer(String s) {
		fApplicationId = Configuration.getPid(s + "." + PAR_APPLICATION);
		fSeedGenerator = new Random(Configuration.getLong(s + "." + PAR_GENERATOR_SEED));
	}
	
	// ----------------------------------------------------------------------
	
	@Override
	public void initialize(Node n) {
		if (n == null) {
			throw new IllegalStateException("Node registry has not been initialized properly.");
		} else if (n.getID() != fNextExpected++) {
			throw new IllegalStateException("ID range must be contiguous.");
		}
		
		SimpleTrafficGenerator app = (SimpleTrafficGenerator) n.getProtocol(fApplicationId);
		app.setTrafficGeneratorSeed(fSeedGenerator.nextLong());
	}
	
	// ----------------------------------------------------------------------

	@Override
	public boolean execute() {
		INodeRegistry registry = NodeRegistry.getInstance();
		
		for (int i = 0; i < Network.size(); i++) {
			Node node = registry.getNode(i);
			initialize(node);
		}
		
		return false;
	}

	// ----------------------------------------------------------------------
}
