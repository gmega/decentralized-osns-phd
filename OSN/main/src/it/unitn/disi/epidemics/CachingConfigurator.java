package it.unitn.disi.epidemics;

import it.unitn.disi.newscasting.internal.IApplicationConfigurator;
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.peersim.CachingResolver;
import peersim.config.Configuration;
import peersim.config.IResolver;
import peersim.config.resolvers.CompositeResolver;

public abstract class CachingConfigurator implements IApplicationConfigurator {

	// ----------------------------------------------------------------------
	// Instance-shared state.
	// ----------------------------------------------------------------------

	/**
	 * Flag used to see if {@link #oneShotConfig(String)} has been called
	 * already or not. It's a hackish PeerSim idiom.
	 */
	private static boolean fConfigured = false;

	/**
	 * Adds a caching layer on top of the PeerSim {@link Configuration}
	 * singleton, otherwise performance becomes unbearable, particularly under
	 * the debugger (not sure why, some bug either in the JVM or the JEP used by
	 * PeerSim).
	 */
	protected static IResolver fResolver;

	public void configure(IProtocolSet set, IResolver resolver, String prefix)
			throws Exception {
		this.oneShotConfigInternal(prefix, resolver);
		this.configure0(set, fResolver, prefix);
	}

	private void oneShotConfigInternal(String prefix, IResolver resolver) {
		if (fConfigured) {
			return;
		}
		// Sets up the resolver first.
		CompositeResolver composite = new CompositeResolver();
		composite.addResolver(resolver);
		fResolver = CachingResolver.cachingResolver(composite
				.asInvocationHandler());

		this.oneShotConfig(prefix, resolver);
		fConfigured = true;
	}

	protected abstract void oneShotConfig(String prefix, IResolver resolver);

	protected abstract void configure0(IProtocolSet set, IResolver resolver,
			String prefix) throws Exception;

	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException ex) {
			throw MiscUtils.nestRuntimeException(ex);
		}
	}
}
