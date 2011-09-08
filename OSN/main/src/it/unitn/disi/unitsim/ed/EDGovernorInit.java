package it.unitn.disi.unitsim.ed;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.Control;

@AutoConfig
public class EDGovernorInit implements Control {
	
	private final EDGovernor fGovernor;
	
	public EDGovernorInit(@Attribute("EDGovernor") EDGovernor governor) {
		fGovernor = governor;
	}

	@Override
	public boolean execute() {
		fGovernor.start();
		return false;
	}

}
