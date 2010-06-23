package it.unitn.disi.application;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;

/**
 * {@link AbsoluteDifferenceConvergence} understands that the behavior has
 * stabilized once the difference in delivered messags between one round and the
 * other falls below a certain value.
 * 
 * @author giuliano
 */
public class AbsoluteDifferenceConvergence implements peersim.core.Control{
	
	// ----------------------------------------------------------------------
	// Configuration parameters keys.
	// ----------------------------------------------------------------------
	
	private static final String PAR_AFTER_ROUND = "after_round";
	
	private static final String PAR_DELTA = "delta";
	
	private static final String PAR_APP = "application";
	
	// ----------------------------------------------------------------------
	// Configuration parameter storage.
	// ----------------------------------------------------------------------
	
	private final int fAfter;
	
	private final int fDelta;
	
	private final int fAppId;
	
	// ----------------------------------------------------------------------
	// State.
	// ----------------------------------------------------------------------
	
	private int fLastValue = Integer.MAX_VALUE;
	
	public AbsoluteDifferenceConvergence(String prefix) {
		fAppId = Configuration.getPid(prefix + "." + PAR_APP);
		fAfter = Configuration.getInt(prefix + "." + PAR_AFTER_ROUND, -1);
		fDelta = Configuration.getInt(prefix + "." + PAR_DELTA, 0);
	}

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
			System.out.print("STABLE:" + CommonState.getTime());
			return true;
		}
		
		fLastValue = totalValue;
		
		return false;
	}
}
