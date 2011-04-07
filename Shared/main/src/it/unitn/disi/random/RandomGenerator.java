package it.unitn.disi.random;

import peersim.config.AutoConfig;
import peersim.config.plugin.IGenericServiceInitializer;
import peersim.config.plugin.PluginContainer;
import peersim.core.CommonState;

@AutoConfig
public class RandomGenerator implements IGenericServiceInitializer {
	
	public static final String KEY = "Random";

	@Override
	public void run(PluginContainer container) {
		container.registerObject(KEY, CommonState.r);
	}
	
}
