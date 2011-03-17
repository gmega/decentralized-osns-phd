package peersim.config.plugin;

public interface IPlugin {
	
	public String id();
	
	public void start();
	
	public void stop();
	
}
