package it.unitn.disi.newscasting.internal;

import it.unitn.disi.ISelectionFilter;
import it.unitn.disi.newscasting.IPeerSelector;
import it.unitn.disi.newscasting.LoggingObserver;
import it.unitn.disi.newscasting.internal.demers.DemersRumorMonger;
import it.unitn.disi.newscasting.internal.forwarding.BloomFilterHistoryFw;
import it.unitn.disi.newscasting.internal.forwarding.HistoryForwarding;
import it.unitn.disi.newscasting.probabrm.ProbabilisticRumorMonger;
import it.unitn.disi.utils.IReference;
import it.unitn.disi.utils.logging.LogManager;
import it.unitn.disi.utils.peersim.FallThroughReference;
import it.unitn.disi.utils.peersim.ProtocolReference;

import java.io.IOException;

import peersim.config.Configuration;
import peersim.config.resolvers.PeerSimResolver;
import peersim.core.CommonState;
import peersim.core.Linkable;

public class NewscastAppConfigurator implements IApplicationConfigurator{
	
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
	private static final String VAL_FORWARDING = "forwarding";
	private static final String VAL_FORWARDING_BLOOM = "use_bloom_filters";
	
	/**
	 * Defines the backing storage implementation.
	 */
	private static final String PAR_STORAGE = "storage";
	
	private static final String VAL_SIMPLE = "simple";
	private static final String VAL_COMPACT = "compact";
	
	private static final String VAL_NONE = "none";
	
	public NewscastAppConfigurator(String prefix) {
	}
	
	public void configure(SocialNewscastingService app, String prefix,
			int protocolId, int socialNetworkId) throws Exception {
		configureStorage(app, prefix);
		configureLogging(app, prefix);
		configureAntiEntropy(app, prefix, protocolId, socialNetworkId);
		configureRumorMongering(app, prefix,protocolId, socialNetworkId);
	}

	private void configureRumorMongering(SocialNewscastingService app,
			String prefix, int protocolId, int socialNetworkId) {
		// Configures the rumor mongering strategy.
		String rmType = Configuration.getString(prefix + "." + PAR_RUMOR_MONGER);
		if(rmType.equals(VAL_DEMERS)) {
			configureDemers(app, prefix, protocolId, socialNetworkId);
		} else if (rmType.equals(VAL_FORWARDING)) {
			configureGreedyDiffusion(app, prefix, protocolId, socialNetworkId,
					Configuration.contains(prefix + "." + PAR_RUMOR_MONGER
							+ "." + VAL_FORWARDING_BLOOM));
		} else if (!rmType.equals(VAL_NONE)) {
			throw new IllegalArgumentException();
		}
	}

	@SuppressWarnings("unchecked")
	private void configureAntiEntropy(SocialNewscastingService app,
			String prefix, int protocolId, int socialNetworkId) {
		
		if (Configuration.contains(prefix + "." + PAR_ANTI_ENTROPY + "."
				+ VAL_NONE)) {
			return;
		}
		
		// Configures the anti-entropy strategy.
		// XXX Anti-entropy is too coupled to the newscast application. This is so by historical reasons (they
		// were the same thing at the beginning) but that should change with time.
		DemersAntiEntropy dae = new DemersAntiEntropy(protocolId, socialNetworkId);
		app.addStrategy(new Class [] { DemersAntiEntropy.class },
				dae, selector(PAR_ANTI_ENTROPY), filter(PAR_ANTI_ENTROPY),
				probability(PAR_ANTI_ENTROPY));
	}

	@SuppressWarnings("unchecked")
	private void configureGreedyDiffusion(SocialNewscastingService app,
			String prefix, int protocolId, int socialNetworkId, boolean histories) {
		HistoryForwarding gd;
		Class [] keys;
		PeerSimResolver resolver = new PeerSimResolver();
		if (histories) {
			gd = new BloomFilterHistoryFw(protocolId, socialNetworkId, resolver, prefix + "." + PAR_RUMOR_MONGER);
			keys = new Class[] { HistoryForwarding.class, BloomFilterHistoryFw.class };
		} else {
			gd = new HistoryForwarding(protocolId, socialNetworkId, resolver, prefix + "." + PAR_RUMOR_MONGER);
			keys = new Class[] { HistoryForwarding.class };
		}
		
		app.addStrategy(keys, gd,
				selector(PAR_RUMOR_MONGER),
				new FallThroughReference<ISelectionFilter>(gd),
				probability(PAR_RUMOR_MONGER));
		app.addSubscriber(gd);
	}

	@SuppressWarnings("unchecked")
	private void configureDemers(SocialNewscastingService app, String prefix,
			int protocolId, int socialNetworkId) {
		DemersRumorMonger demersRm = new DemersRumorMonger(prefix + "."
				+ PAR_RUMOR_MONGER, protocolId, new ProtocolReference<Linkable>(socialNetworkId), CommonState.r);
		app.addStrategy(new Class[] { DemersRumorMonger.class }, demersRm,
				selector(PAR_RUMOR_MONGER), filter(PAR_RUMOR_MONGER),
				probability(PAR_RUMOR_MONGER));

		// Rumor mongering wants to know of new events.
		app.addSubscriber(demersRm);
	}
	
	private void configureStorage(SocialNewscastingService service, String prefix) {
		String type = Configuration.getString(prefix + "." + PAR_STORAGE, VAL_SIMPLE);
		
		IWritableEventStorage storage;
		if (type.equals(VAL_COMPACT)) {
			storage = new CompactEventStorage();
		} else if (type.equals(VAL_SIMPLE)) {
			storage = new SimpleEventStorage();
		} else {
			throw new IllegalArgumentException();
		}
		
		service.setStorage(storage);
	}
	
	private void configureLogging(SocialNewscastingService service, String prefix) throws IOException {
		LogManager mgr = LogManager.getInstance();
		String logId = mgr.addUnique(prefix);
		
		if (logId != null) {
			LoggingObserver observer = new LoggingObserver(mgr.get(logId), false);
			service.addSubscriber(observer);
		}
	}
	
	private IReference<IPeerSelector> selector(String prefix) {
		return new ProtocolReference<IPeerSelector>(Configuration
				.getPid(prefix + "." + prefix + "." + PAR_SELECTOR));
	}
	
	private IReference<ISelectionFilter> filter(String prefix) {
		String filterName = prefix + "." + prefix + "." + PAR_FILTER;
		if (!Configuration.contains(filterName)) {
			return new FallThroughReference<ISelectionFilter>(ISelectionFilter.ALWAYS_TRUE_FILTER);
		}
		
		return new ProtocolReference<ISelectionFilter>(Configuration
				.getPid(filterName));
	}

	private double probability(String prefix) {
		String percentageName = prefix + "." + prefix + "." + PAR_PERCENTAGE;
		if (!Configuration.contains(percentageName)) {
			return 1.0;
		}
		
		return Double.parseDouble(Configuration.getString(percentageName));
	}

	public Object clone () {
		return this;
	}
}
