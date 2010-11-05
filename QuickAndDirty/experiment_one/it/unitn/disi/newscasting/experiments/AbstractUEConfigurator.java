package it.unitn.disi.newscasting.experiments;

import it.unitn.disi.ISelectionFilter;
import it.unitn.disi.newscasting.IContentExchangeStrategy;
import it.unitn.disi.newscasting.IPeerSelector;
import it.unitn.disi.newscasting.internal.IApplicationConfigurator;
import it.unitn.disi.newscasting.internal.IEventObserver;
import it.unitn.disi.newscasting.internal.IWritableEventStorage;
import it.unitn.disi.newscasting.internal.SocialNewscastingService;
import it.unitn.disi.utils.peersim.CachingResolver;
import it.unitn.disi.utils.peersim.FallThroughReference;
import peersim.config.IResolver;
import peersim.config.resolvers.CompositeResolver;
import peersim.config.resolvers.PeerSimResolver;

/**
 * Abstract {@link IApplicationConfigurator} implementation for the unit
 * experiment framework.
 * 
 * @author giuliano
 */
public abstract class AbstractUEConfigurator implements
		IApplicationConfigurator {

	protected IResolver fResolver = CachingResolver
			.cachingResolver(new CompositeResolver(new PeerSimResolver()));

	@Override
	public void configure(SocialNewscastingService app, String prefix,
			int protocolId, int socialNetworkId) throws Exception {

		// Application storage.
		app.setStorage(storage(prefix, protocolId, socialNetworkId));

		// Unit experiment exchange strategy.
		IContentExchangeStrategy strategy = strategy(prefix, protocolId,
				socialNetworkId);

		// The peer selector.
		IPeerSelector selector = selector(prefix, protocolId, socialNetworkId);

		// And the selection filter.
		ISelectionFilter filter = filter(prefix, protocolId, socialNetworkId);

		app.addStrategy(classes(), strategy,
				new FallThroughReference<IPeerSelector>(selector),
				new FallThroughReference<ISelectionFilter>(filter), 1.0);
		
		if (strategy instanceof IEventObserver) {
			app.addSubscriber((IEventObserver) strategy);
		}
		
		app.addSubscriber(ExperimentStatisticsManager.getInstance());
	}

	protected IWritableEventStorage storage(String prefix, int protocolId,
			int socialNetworkId) {
		// The default storage class for unit experiments is the single message
		// storage.
		return new SingleEventStorage();
	}

	protected abstract IPeerSelector selector(String prefix, int protocolId,
			int socialNetworkId);

	protected abstract ISelectionFilter filter(String prefix, int protocolId,
			int socialNetworkId);

	protected abstract Class<? extends IContentExchangeStrategy>[] classes();

	protected abstract IContentExchangeStrategy strategy(String prefix,
			int protocolId, int socialNetworkId);

	@Override
	public Object clone() {
		return this;
	}
}
