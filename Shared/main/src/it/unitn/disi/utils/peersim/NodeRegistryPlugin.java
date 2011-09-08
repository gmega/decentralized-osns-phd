package it.unitn.disi.utils.peersim;

import peersim.config.AutoConfig;
import peersim.config.plugin.IGenericServiceInitializer;
import peersim.config.plugin.PluginContainer;

@AutoConfig
public class NodeRegistryPlugin implements IGenericServiceInitializer {

	@Override
	public void run(PluginContainer container) {
		container.registerObject("NodeRegistry", NodeRegistry.getInstance());
	}

}
