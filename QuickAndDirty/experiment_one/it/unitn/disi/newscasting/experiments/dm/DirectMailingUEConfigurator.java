package it.unitn.disi.newscasting.experiments.dm;

import it.unitn.disi.epidemics.IApplicationConfigurator;
import it.unitn.disi.epidemics.IContentExchangeStrategy;
import it.unitn.disi.epidemics.IPeerSelector;
import it.unitn.disi.epidemics.ISelectionFilter;
import it.unitn.disi.newscasting.experiments.configurators.AbstractUEConfigurator;
import it.unitn.disi.newscasting.internal.SocialNewscastingService;
import it.unitn.disi.newscasting.internal.selectors.RandomSelectorOverLinkable;
import peersim.config.AutoConfig;

@AutoConfig
public class DirectMailingUEConfigurator extends AbstractUEConfigurator implements
		IApplicationConfigurator {

	private DirectMailingUE fStrategy;

	// ----------------------------------------------------------------------

	public DirectMailingUEConfigurator() {
	}

	// ----------------------------------------------------------------------

	@Override
	protected IContentExchangeStrategy strategy(SocialNewscastingService app,
			String prefix, int protocolId, int socialNetworkId) {
		fStrategy = new DirectMailingUE(socialNetworkId, protocolId);
		return fStrategy;
	}

	// ----------------------------------------------------------------------

	@Override
	protected IPeerSelector selector(SocialNewscastingService app,
			String prefix, int protocolId, int socialNetworkId) {
		return new RandomSelectorOverLinkable(fResolver, prefix);
	}

	// ----------------------------------------------------------------------

	@Override
	protected ISelectionFilter filter(SocialNewscastingService app,
			String prefix, int protocolId, int socialNetworkId) {
		return (ISelectionFilter) fStrategy;
	}

	// ----------------------------------------------------------------------

	@Override
	@SuppressWarnings("unchecked")
	protected Class<? extends IContentExchangeStrategy>[] classes() {
		return new Class[] { DirectMailingUE.class };
	}

	// ----------------------------------------------------------------------

	public Object clone() {
		return this;
	}

	// ----------------------------------------------------------------------
}