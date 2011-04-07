package peersim.config.plugin;

public interface IPluginDescriptor {
	
	public String id();
	
	public String configurationPrefix();
	
	public String [] depends();
	
	public Class<?> pluginClass();
	
}
