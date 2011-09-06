package it.unitn.disi.test.framework;

import peersim.core.CommonState;
import peersim.core.Control;
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

		for (int i = 0; i < size; i++) {
			Node node = Network.get(i);
			node.setFailState(available(i, time) ? Node.OK : Node.DOWN);
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
