package it.unitn.disi.f2f;

import peersim.config.AutoConfig;
import peersim.core.Control;

@AutoConfig
public class JoinControl implements Control {

	private boolean fBootstrap;
	
	@Override
	public boolean execute() {
		JoinExperimentGovernor controller = DiscoveryProtocolConfigurator.controller();
		if (!fBootstrap) {
			controller.runNext();
			fBootstrap = true;
		}
		return !controller.running();
	}

}
