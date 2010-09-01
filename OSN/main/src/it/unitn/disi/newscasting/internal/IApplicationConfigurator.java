package it.unitn.disi.newscasting.internal;


/**
 * {@link IApplicationConfigurator} knows how to configure the
 * {@link SocialNewscastingService}.
 */
public interface IApplicationConfigurator {
	/**
	 * Configures the application. Configurator might assume that the instance
	 * is clean; i.e., has no configured strategies whatsoever.
	 */
	void configure(SocialNewscastingService app, int protocolId, int socialNetworkId);
}

