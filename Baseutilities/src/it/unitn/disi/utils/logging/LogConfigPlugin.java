package it.unitn.disi.utils.logging;

import org.apache.log4j.BasicConfigurator;

import peersim.config.AutoConfig;
import peersim.config.plugin.IGenericServiceInitializer;
import peersim.config.plugin.PluginContainer;

@AutoConfig
public class LogConfigPlugin implements IGenericServiceInitializer {

	@Override
	public void run(PluginContainer container) {
		BasicConfigurator.configure();
	}

}
