package it.unitn.disi.epidemics;

import it.unitn.disi.newscasting.internal.SocialNewscastingService;
import peersim.config.IResolver;
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
	void configure(IProtocolSet app, IResolver resolver,
			String prefix) throws Exception;
}
