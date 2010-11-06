package it.unitn.disi.newscasting.experiments.configurators;

import it.unitn.disi.ISelectionFilter;
import it.unitn.disi.newscasting.IContentExchangeStrategy;
import it.unitn.disi.newscasting.IPeerSelector;
import it.unitn.disi.newscasting.internal.IApplicationConfigurator;
import it.unitn.disi.newscasting.internal.SocialNewscastingService;
import it.unitn.disi.newscasting.internal.demers.DemersRumorMonger;
import it.unitn.disi.newscasting.internal.selectors.RandomSelectorOverLinkable;
import it.unitn.disi.utils.peersim.ProtocolReference;
import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.CommonState;
import peersim.core.Linkable;

/**
 * Configuration for Demers Rumor mongering under the unit experiment framework.
 * 
 * @author giuliano
 */
@AutoConfig
public class DemersConfigurator extends AbstractUEConfigurator implements
		IApplicationConfigurator {

	// ----------------------------------------------------------------------

	public DemersConfigurator(@Attribute(Attribute.PREFIX) String prefix) {
		super(prefix);
	}

	// ----------------------------------------------------------------------

	@Override
	protected IContentExchangeStrategy strategy(SocialNewscastingService app,
			String prefix, int protocolId, int socialNetworkId) {
		return new DemersRumorMonger(fResolver, prefix, protocolId,
				new ProtocolReference<Linkable>(fResolver.getInt(prefix,
						"linkable")), CommonState.r);
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
		return ISelectionFilter.ALWAYS_TRUE_FILTER;
	}

	// ----------------------------------------------------------------------

	@Override
	@SuppressWarnings("unchecked")
	protected Class<? extends IContentExchangeStrategy>[] classes() {
		return new Class[] { DemersRumorMonger.class };
	}

	// ----------------------------------------------------------------------

	public Object clone() {
		return this;
	}

	// ----------------------------------------------------------------------
}
