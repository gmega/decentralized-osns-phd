package peersim.config.plugin;

/**
 * A {@link IGenericServiceInitializer} differs from a plug-in in that it is not
 * a service itself. Rather, it adds simple services to the
 * {@link PluginContainer} by calling
 * {@link PluginContainer#registerObject(String, Object)}. Such objects won't
 * have any kind of lifecycle or dependency management.
 * 
 * @author giuliano
 */
public interface IGenericServiceInitializer {
	/**
	 * Runs the configuration code.
	 * 
	 * @param container
	 *            the parent {@link PluginContainer}.
	 */
	public void run(PluginContainer container);
}
