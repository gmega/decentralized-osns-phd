package it.unitn.disi.newscasting.experiments;

import java.util.HashMap;

import it.unitn.disi.ISelectionFilter;
import it.unitn.disi.newscasting.IContentExchangeStrategy;
import it.unitn.disi.newscasting.IPeerSelector;
import it.unitn.disi.newscasting.internal.IApplicationConfigurator;
import it.unitn.disi.newscasting.internal.IWritableEventStorage;
import it.unitn.disi.newscasting.internal.SocialNewscastingService;
import it.unitn.disi.utils.peersim.FallThroughReference;

public abstract class AbstractUEConfigurator implements
		IApplicationConfigurator {

	

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
