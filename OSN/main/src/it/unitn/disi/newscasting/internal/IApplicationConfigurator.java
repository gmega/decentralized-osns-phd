package it.unitn.disi.newscasting.internal;

import peersim.core.Protocol;


/**
 * {@link IApplicationConfigurator} knows how to configure the
 * {@link SocialNewscastingService}.
 */
public interface IApplicationConfigurator extends Protocol {
	/**
	 * Configures the application. Configurator might assume that the instance
	 * is clean; i.e., has no configured strategies whatsoever.
	 */
	void configure(SocialNewscastingService app, int protocolId, int socialNetworkId);
}

