package it.unitn.disi.newscasting.internal.demers;

import it.unitn.disi.application.SimpleApplication;
import it.unitn.disi.newscasting.IApplicationInterface;
import it.unitn.disi.newscasting.Tweet;
import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

@AutoConfig
public class ResidueConvergenceController implements Control{

	@Attribute("sns")
	private int fSocialNewscastingId;
	
	@Attribute("application")
	private int fApplicationId;

	@Attribute(value = "epsilon", defaultValue = "0.00001")
	private double fEpsilon;
	
	private double fLastResidue;
	
	public ResidueConvergenceController() {
	}

	@Override
	public boolean execute() {
		
		// Finds the tweeting node.
		Node tweeting = null;
		for (int i = 0; i < Network.size(); i++) {
			Node candidate = Network.get(i);
			SimpleApplication app = (SimpleApplication) Network.get(i).getProtocol(fApplicationId);
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
			IApplicationInterface app = (IApplicationInterface) Network.get(i).getProtocol(fSocialNewscastingId);
			if (app.storage().contains(tweet)) {
				residue--;
			}
		}
		
		return residue /= Network.size();
	}
}
