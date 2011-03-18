package peersim.config.plugin;

import peersim.config.IResolver;

public interface IPlugin {
	
	public String id();
	
	public void start(IResolver resolver) throws Exception;
	
	public void stop() throws Exception;
	
}
