package it.unitn.disi.test.framework;


import java.util.ArrayList;

import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Fallible;
import peersim.core.Network;
import peersim.core.Node;

/**
 * Simple control for replaying churn patterns in testing scenarios.
 * 
 * @author giuliano
 */
public class UpDownControl implements Control {
	
	private final boolean[][] fAvailability;

	public UpDownControl(boolean[][] availability) {
		fAvailability = availability;
	}

	@Override
	public boolean execute() {
		int size = Network.size();
		int time = CommonState.getIntTime();

		ArrayList<Node> ups = new ArrayList<Node>();
		ArrayList<Node> downs = new ArrayList<Node>();
	
		for (int i = 0; i < size; i++) {
			Node node = Network.get(i);
			if(available(i, time)) {
				ups.add(node);				
			} else {
				downs.add(node);
			}
		}
		
		// Applies first downs, then ups, so as to not
		// inflate network connectivity.
		for (Node node : downs) {
			node.setFailState(Fallible.DOWN);
		}
		
		for (Node node : ups) {
			node.setFailState(Fallible.OK);
		}
		
		return false;
	}

	private boolean available(int i, int time) {
		if (i >= fAvailability.length) {
			return false;
		}
		
		if (time >= fAvailability[i].length) {
			return false;
		}
		
		return fAvailability[i][time];
	}
}
