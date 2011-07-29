package peersim.config.plugin;

import peersim.config.IResolver;

/**
 * An {@link IPlugin} is a user-programmable extension to PeerSim.
 * 
 * @author giuliano
 */
public interface IPlugin {

	public String id();

	public void start(IResolver resolver) throws Exception;

	public void stop() throws Exception;

}
