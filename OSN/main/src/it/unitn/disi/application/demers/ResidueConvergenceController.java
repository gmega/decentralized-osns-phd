package it.unitn.disi.application.demers;

import it.unitn.disi.application.IApplication;
import it.unitn.disi.application.Tweet;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

public class ResidueConvergenceController implements Control{
	
	private static final String PAR_APPLICATION = "application";
	
	private static final String PAR_EPSILON = "epsilon";
	
	private final int fApplicationId;

	private final double fEpsilon;
	
	private double fLastResidue;
	
	public ResidueConvergenceController(String prefix) {
		fApplicationId = Configuration.getPid(prefix + "." + PAR_APPLICATION);
		fEpsilon = Configuration.getDouble(prefix + "." + PAR_EPSILON, 0.00001);
	}
	@Override
	public boolean execute() {
		
		// Finds the tweeting node.
		Node tweeting = null;
		for (int i = 0; i < Network.size(); i++) {
			Node candidate = Network.get(i);
			IApplication app = (IApplication) Network.get(i).getProtocol(fApplicationId);
			if (!app.isSuppressingTweets()) {
				tweeting = candidate; 
				break;
			}
		}
		
		if (tweeting == null) {
			throw new IllegalStateException();
		}
		
		double residue = computeResidue(new Tweet(tweeting, 1));
		System.out.println("RESIDUE: " + residue);
		
		if (Math.abs(residue - fLastResidue) < fEpsilon) {
			System.out.println("STABLE: " + CommonState.getTime());
			return true;
		}
		
		fLastResidue = residue;
		
		return false;
	}

	
	private double computeResidue(Tweet tweet) {
		double residue = Network.size();
		for (int i = 0; i < Network.size(); i++) {
			IApplication app = (IApplication) Network.get(i).getProtocol(fApplicationId);
			if (app.knows(tweet)) {
				residue--;
			}
		}
		
		return residue /= Network.size();
	}
}
