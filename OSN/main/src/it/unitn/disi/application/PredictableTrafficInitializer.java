package it.unitn.disi.application;

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
 * @author giuliano
 */
public class PredictableTrafficInitializer implements Control, NodeInitializer {
	
	private static final String PAR_APPLICATION = "application";
	
	private static final String PAR_GENERATOR_SEED = "seed";

	private final int fApplicationId;
	
	private final Random fSeedGenerator;
	
	public PredictableTrafficInitializer(String s) {
		fApplicationId = Configuration.getPid(s + "." + PAR_APPLICATION);
		fSeedGenerator = new Random(Configuration.getLong(s + "." + PAR_GENERATOR_SEED));
	}
	
	@Override
	public void initialize(Node n) {
		IApplication app = (IApplication) n.getProtocol(fApplicationId);
		app.setTrafficGeneratorSeed(fSeedGenerator.nextLong());
	}

	@Override
	public boolean execute() {
		for (int i = 0; i < Network.size(); i++) {
			Node node = Network.get(i);
			initialize(node);
		}
		
		return false;
	}

}
