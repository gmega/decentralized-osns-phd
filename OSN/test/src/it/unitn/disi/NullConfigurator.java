package it.unitn.disi;

import peersim.config.IResolver;
import it.unitn.disi.epidemics.IApplicationConfigurator;
import it.unitn.disi.epidemics.IProtocolSet;

public class NullConfigurator implements IApplicationConfigurator {

	@Override
	public void configure(IProtocolSet app, IResolver resolver, String prefix)
			throws Exception {

	}
	
	public Object clone() {
		return this;
	}

}
