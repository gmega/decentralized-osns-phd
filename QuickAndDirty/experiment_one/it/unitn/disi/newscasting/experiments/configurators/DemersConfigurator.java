package it.unitn.disi.newscasting.experiments.configurators;

import it.unitn.disi.epidemics.IApplicationConfigurator;
import it.unitn.disi.epidemics.IContentExchangeStrategy;
import it.unitn.disi.epidemics.IPeerSelector;
import it.unitn.disi.epidemics.ISelectionFilter;
import it.unitn.disi.newscasting.experiments.f2f.CoverageAnalyzer;
import it.unitn.disi.newscasting.internal.SocialNewscastingService;
import it.unitn.disi.newscasting.internal.demers.DemersRumorMonger;
import it.unitn.disi.newscasting.internal.selectors.RandomSelectorOverLinkable;
import it.unitn.disi.unitsim.CDGovernor;
import peersim.config.AutoConfig;
import peersim.config.IResolver;
import peersim.config.MissingParameterException;
import peersim.config.ObjectCreator;
import peersim.core.CommonState;
import peersim.core.Node;

/**
 * Configuration for Demers Rumor mongering under the unit experiment framework.
 * 
 * @author giuliano
 */
@AutoConfig
public class DemersConfigurator extends AbstractUEConfigurator implements
		IApplicationConfigurator {

	private DemersRumorMonger fStrategy;

	// ----------------------------------------------------------------------

	public DemersConfigurator() {
	}

	// ----------------------------------------------------------------------

	@Override
	protected void oneShotConfig(String prefix, IResolver resolver) {
		super.oneShotConfig(prefix, resolver);
		try {
			resolver.getString(prefix, "use_ca");
		} catch (MissingParameterException ex) {
			return;
		}
		CoverageAnalyzer analyzer = ObjectCreator.createInstance(
				CoverageAnalyzer.class, prefix + ".ca", fResolver);
				
		CDGovernor governor = (CDGovernor) resolver.getObject(
				IResolver.NULL_KEY, CDGovernor.class.getSimpleName());
		
		governor.addExperimentObserver(analyzer);
	}

	// ----------------------------------------------------------------------

	@Override
	protected IContentExchangeStrategy strategy(SocialNewscastingService app,
			String prefix, int protocolId, int socialNetworkId) {
		Node node = app.node();
		fStrategy = new DemersRumorMonger(fResolver, prefix, protocolId, node,
				CommonState.r, true);
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
		return new Class[] { DemersRumorMonger.class };
	}

	// ----------------------------------------------------------------------

	public Object clone() {
		return this;
	}

	// ----------------------------------------------------------------------
}
