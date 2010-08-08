package it.unitn.disi.application;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;

/**
 * {@link AbsoluteDifferenceConvergence} understands that the behavior of an
 * {@link IApplication} to be stable once the difference in delivered messages
 * between one round and the other falls below a certain value, for more than 
 * a certain number of rounds.
 * 
 * @author giuliano
 */
@AutoConfig
public class AbsoluteDifferenceConvergence implements peersim.core.Control{

	// ----------------------------------------------------------------------
	// Configuration parameter storage.
	// ----------------------------------------------------------------------

	@Attribute("after")
	private int fAfter;

	@Attribute(value = "delta", defaultValue = "0")
	private int fDelta;
	
	@Attribute(value = "atleastfor", defaultValue = "1")
	private int fAtLeast;
	
	@Attribute("application")
	private int fAppId;
	
	// ----------------------------------------------------------------------
	// State.
	// ----------------------------------------------------------------------
	
	private int fLastValue = Integer.MAX_VALUE;
	
	private int fRemainingRounds = 0;
	
	public AbsoluteDifferenceConvergence() { }

	@Override
	public boolean execute() {
		int totalValue = 0;
		
		for (int i = 0; i < Network.size(); i++) {
			Node node = Network.get(i);
			IApplication app = (IApplication) node.getProtocol(fAppId);
			totalValue += app.pendingReceives();
		}
		
		if (CommonState.getTime() < fAfter) {
			return false;
		}
		
		if (Math.abs(fLastValue - totalValue) <= fDelta) {
			fRemainingRounds++;
		} else {
			fRemainingRounds = 0;
		}
		
		if (fRemainingRounds >= fAtLeast) {
			System.out.print("STABLE:" + CommonState.getTime());
			return true;
		}
		
		fLastValue = totalValue;
		
		return false;
	}
}
