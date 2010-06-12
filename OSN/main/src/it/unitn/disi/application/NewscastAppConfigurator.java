package it.unitn.disi.application;

import it.unitn.disi.application.demers.DemersRumorMonger;
import it.unitn.disi.application.greedydiffusion.GreedyDiffusion;
import it.unitn.disi.application.interfaces.IPeerSelector;
import it.unitn.disi.application.interfaces.ISelectionFilter;
import it.unitn.disi.application.probabrm.ProbabilisticRumorMonger;
import it.unitn.disi.utils.FallThroughReference;
import it.unitn.disi.utils.IReference;
import it.unitn.disi.utils.ProtocolReference;
import peersim.config.Configuration;
import peersim.core.CommonState;

class NewscastAppConfigurator implements IApplicationConfigurator{
	
	/**
	 * Reference to the component computation service.
	 */
	private static final String PAR_CCSID = "component_computer";
	
	/**
	 * Prefix for parameters related to anti entropy.
	 */
	private static final String PAR_ANTI_ENTROPY = "ae";
	
	/**
	 * Percentage (expected) of the rounds in which a strategy is to be run.
	 */
	private static final String PAR_PERCENTAGE = "percentage";

	/**
	 * Selector for a strategy.
	 */
	private static final String PAR_SELECTOR = "selector";
	
	/**
	 * Filter for selector.
	 */
	private static final String PAR_FILTER = "filter";
	
	/**
	 * Defines the type of rumor mongering to use. Might be set to:
	 * 
	 * <ol>
	 * <li> {@link #VAL_DEMERS}: uses {@link DemersRumorMonger}.</li>
	 * <li> {@link #VAL_PROBABILISTIC}: uses {@link ProbabilisticRumorMonger}.</li>
	 * </ol>
	 */
	private static final String PAR_RUMOR_MONGER = "rumor";
	private static final String VAL_DEMERS = "demers";
	private static final String VAL_GREEDY = "greedy";
	private static final String VAL_NONE = "none";
	
	private final String fPrefix;
	
	public NewscastAppConfigurator(String prefix) {
		fPrefix = prefix;
	}
	
	public void configure(NewscastApplication app, int protocolId, int socialNetworkId) {
		// Configures the anti-entropy strategy.
		// XXX Anti-entropy is too coupled to the newscast application. This is so by historical reasons (they
		// were the same thing at the beginning) but that should change with time.
		DemersAntiEntropy dae = new DemersAntiEntropy(protocolId, socialNetworkId);
		app.addStrategy(dae, selector(PAR_ANTI_ENTROPY), filter(PAR_ANTI_ENTROPY), probability(PAR_ANTI_ENTROPY));
		// Creates the event channel for anti entropy.
		BroadcastBus ae = new BroadcastBus();
		app.setAdapter(IMergeObserver.class, DemersAntiEntropy.class, ae);
		// XXX Anti-entropy is the ONLY strategy that gets plugged directly into the 
		// application internal observer. 
		// TODO fix the design so this is no longer the case.
		ae.addSubscriber(app.internalObserver());
						
		// Configures the rumor mongering strategy.
		String rmType = Configuration.getString(fPrefix + "." + PAR_RUMOR_MONGER);
		if(rmType.equals(VAL_DEMERS)) {
			configureDemers(app, protocolId, socialNetworkId);
		} else if (rmType.equals(VAL_GREEDY)) {
			configureGreedyDiffusion(app, protocolId, socialNetworkId);
		} else if (!rmType.equals(VAL_NONE)) {
			throw new IllegalArgumentException();
		} 
	}

	private void configureGreedyDiffusion(NewscastApplication app,
			int protocolId, int socialNetworkId) {
		GreedyDiffusion gd = new GreedyDiffusion(protocolId, socialNetworkId, fPrefix + "." + PAR_RUMOR_MONGER);
		app.setAdapter(GreedyDiffusion.class, null, gd);
		app.addStrategy(gd, selector(PAR_RUMOR_MONGER),
				new FallThroughReference<ISelectionFilter>(gd),
				probability(PAR_RUMOR_MONGER));
		app.addSubscriber(gd);
	}

	private void configureDemers(NewscastApplication app, int protocolId,
			int socialNetworkId) {
		DemersRumorMonger demersRm = new DemersRumorMonger(fPrefix + "."
				+ PAR_RUMOR_MONGER, protocolId, socialNetworkId, CommonState.r);
		app.setAdapter(DemersRumorMonger.class, null, demersRm);
		app.addStrategy(demersRm, selector(PAR_RUMOR_MONGER), filter(PAR_RUMOR_MONGER),  probability(PAR_RUMOR_MONGER));
		// Rumor mongering wants to know of new events.
		app.addSubscriber(demersRm);
	}
	
	private IReference<IPeerSelector> selector(String prefix) {
		return new ProtocolReference<IPeerSelector>(Configuration
				.getPid(fPrefix + "." + prefix + "." + PAR_SELECTOR));
	}
	
	private IReference<ISelectionFilter> filter(String prefix) {
		String filterName = fPrefix + "." + prefix + "." + PAR_FILTER;
		if (!Configuration.contains(filterName)) {
			return new FallThroughReference<ISelectionFilter>(ISelectionFilter.ALWAYS_TRUE_FILTER);
		}
		
		return new ProtocolReference<ISelectionFilter>(Configuration
				.getPid(filterName));
	}

	private double probability(String prefix) {
		String percentageName = fPrefix + "." + prefix + "." + PAR_PERCENTAGE;
		if (!Configuration.contains(percentageName)) {
			return 1.0;
		}
		
		return Double.parseDouble(Configuration.getString(percentageName));
	}

}
