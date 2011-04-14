package it.unitn.disi.newscasting.experiments.churn;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

@AutoConfig
public class ClockTicker implements Control {

	@Attribute("timeout_controller")
	public int fControllerId;
	
	@Override
	public boolean execute() {
		for (int i = 0; i < Network.size(); i++) {
			Node node = Network.get(i);
			TimeoutController ctl = (TimeoutController) node.getProtocol(fControllerId);
			ctl.tick(node);
		}
		return false;
	}

}
