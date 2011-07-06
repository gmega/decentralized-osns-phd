package it.unitn.disi.newscasting.experiments.configurators;

import it.unitn.disi.epidemics.ProtocolRunner;
import it.unitn.disi.newscasting.experiments.churn.TimeoutController;
import it.unitn.disi.newscasting.experiments.churn.TimeoutReset;
import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

@AutoConfig
public class TimeoutInit implements Control {

	@Attribute("runner")
	private int fRunnerId;
	
	@Attribute("timeout_id")
	private int fTimeoutPid;
		
	@Override
	public boolean execute() {
		for (int i = 0; i < Network.size(); i++) {
			Node node = Network.get(i);
			TimeoutReset resetter = new TimeoutReset(fTimeoutPid);
			ProtocolRunner runner = (ProtocolRunner) node.getProtocol(fRunnerId);
			runner.addSubscriber(resetter);
		}
		return false;
	}
}
