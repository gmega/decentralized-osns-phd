package it.unitn.disi.network;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

/**
 * Tracks the percentage of the rounds - across the whole simulation - for which
 * each node remained up.
 * 
 * @author giuliano
 */
@AutoConfig
public class UptimePercentageTracker implements Control {
	
	/**
	 * ID of the {@link GenericValueHolder} we are using to track the uptime.
	 */
	@Attribute("uptime_holder")
	private int fUptimeHolder;
	
	/**
	 * If present, causes this control to produce a per-node dump of uptime at
	 * the last round of the simulation.
	 */
	@Attribute("dump")
	private boolean fDump;
	
	@Attribute(value = "separator", defaultValue = "UPTIMEDUMP")
	private String fSeparator;
	
	public UptimePercentageTracker() { }
	
	@Override
	public boolean execute() {
		int nSize = Network.size();
		double uptimePercentage = 0.0;

		for (int i = 0; i < nSize; i++) {
			uptimePercentage += incUptime(i);
		}

		int currentRound = CommonState.getIntTime();		
		// The average uptime percentage is given by the total number of "up"
		// rounds divided by the number of rounds, and the number of nodes.
		uptimePercentage /= currentRound*Network.size();
		
		// Performs the dump, if at the last round.
		if (fDump && (currentRound == CommonState.getEndTime() - 1)) {
			System.out.println(fSeparator);
			for (int i = 0; i < nSize; i++) {
				Node node = Network.get(i);				
				double uptime = getUptime(i) / ((double)currentRound);
				System.out.println(node.getID() + " " + uptime);
			}
			System.out.println(fSeparator);
		}
		
		return false;
	}
	
	private GenericValueHolder getHolder(int nodeIndex) {
		Node node = Network.get(nodeIndex);
		return (GenericValueHolder) node.getProtocol(fUptimeHolder);
	}
	
	private int incUptime(int nodeIndex) {
		GenericValueHolder holder = getHolder(nodeIndex);
		int oldValue = (Integer) holder.getValue();
		holder.setValue(oldValue + 1);
		
		return oldValue;
	}
	
	private int getUptime(int nodeIndex) {
		GenericValueHolder holder = getHolder(nodeIndex);
		return (Integer) holder.getValue();
	}

}
