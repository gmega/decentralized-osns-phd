package it.unitn.disi.newscasting.internal;

import it.unitn.disi.ISelectionFilter;
import it.unitn.disi.newscasting.IPeerSelector;
import it.unitn.disi.newscasting.LoggingObserver;
import it.unitn.disi.newscasting.internal.demers.DemersRumorMonger;
import it.unitn.disi.newscasting.internal.forwarding.BloomFilterHistoryFw;
import it.unitn.disi.newscasting.internal.forwarding.HistoryForwarding;
import it.unitn.disi.newscasting.probabrm.ProbabilisticRumorMonger;
import it.unitn.disi.utils.IReference;
import it.unitn.disi.utils.logging.StreamManager;
import it.unitn.disi.utils.peersim.FallThroughReference;
import it.unitn.disi.utils.peersim.ProtocolReference;

import java.io.IOException;
import java.io.OutputStream;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.Configuration;
import peersim.config.IResolver;
import peersim.config.resolvers.ConfigContainerResolver;
import peersim.core.CommonState;
import peersim.core.Linkable;

@AutoConfig
public class NewscastAppConfigurator implements IApplicationConfigurator {

	/**
	 * Reference to the component computation service.
	 */
	private static final String PAR_CCSID = "component_computer";

	/**
	 * Prefix for parameters related to anti entropy.
	 */
	private static final String PAR_ANTI_ENTROPY = "ae";

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

	private StreamManager fManager;

	public NewscastAppConfigurator(
			@Attribute("StreamManager") StreamManager manager) {
		fManager = manager;
	}

	public void configure(SocialNewscastingService app, IResolver resolver,
			String prefix, int protocolId, int socialNetworkId)
			throws Exception {
		configureStorage(app, prefix, socialNetworkId);
		configureLogging(app, resolver, prefix);
		configureAntiEntropy(app, prefix, protocolId, socialNetworkId);
		configureRumorMongering(app, resolver, prefix, protocolId,
				socialNetworkId);
	}

	private void configureRumorMongering(SocialNewscastingService app,
			IResolver resolver, String prefix, int protocolId,
			int socialNetworkId) {
		// Configures the rumor mongering strategy.
		String rmType = Configuration
				.getString(prefix + "." + PAR_RUMOR_MONGER);
		if (rmType.equals(VAL_DEMERS)) {
			configureDemers(app, resolver, prefix, protocolId, socialNetworkId);
		} else if (rmType.equals(VAL_FORWARDING)) {
			configureGreedyDiffusion(
					app,
					resolver,
					prefix,
					protocolId,
					socialNetworkId,
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
		// XXX Anti-entropy is too coupled to the newscast application. This is
		// so by historical reasons (they
		// were the same thing at the beginning) but that should change with
		// time.
		DemersAntiEntropy dae = new DemersAntiEntropy(protocolId,
				socialNetworkId);
		app.addStrategy(new Class[] { DemersAntiEntropy.class }, dae,
				selector(PAR_ANTI_ENTROPY), filter(PAR_ANTI_ENTROPY));
	}

	@SuppressWarnings("unchecked")
	private void configureGreedyDiffusion(SocialNewscastingService app,
			IResolver resolver, String prefix, int protocolId,
			int socialNetworkId, boolean histories) {
		HistoryForwarding gd;

		@SuppressWarnings("rawtypes")
		Class[] keys;
		if (histories) {
			gd = new BloomFilterHistoryFw(protocolId, socialNetworkId,
					resolver, prefix + "." + PAR_RUMOR_MONGER);
			keys = new Class[] { HistoryForwarding.class,
					BloomFilterHistoryFw.class };
		} else {
			gd = new HistoryForwarding(protocolId, socialNetworkId, resolver,
					prefix + "." + PAR_RUMOR_MONGER);
			keys = new Class[] { HistoryForwarding.class };
		}

		app.addStrategy(keys, gd, selector(PAR_RUMOR_MONGER),
				new FallThroughReference<ISelectionFilter>(gd));
		app.addSubscriber(gd);
	}

	@SuppressWarnings("unchecked")
	private void configureDemers(SocialNewscastingService app,
			IResolver resolver, String prefix, int protocolId,
			int socialNetworkId) {
		DemersRumorMonger demersRm = new DemersRumorMonger(resolver, prefix
				+ "." + PAR_RUMOR_MONGER, protocolId,
				new ProtocolReference<Linkable>(socialNetworkId), CommonState.r);
		app.addStrategy(new Class[] { DemersRumorMonger.class }, demersRm,
				selector(PAR_RUMOR_MONGER), filter(PAR_RUMOR_MONGER));

		// Rumor mongering wants to know of new events.
		app.addSubscriber(demersRm);
	}

	private void configureStorage(SocialNewscastingService service,
			String prefix, int snId) {
		String type = Configuration.getString(prefix + "." + PAR_STORAGE,
				VAL_SIMPLE);

		IWritableEventStorage storage;
		if (type.equals(VAL_COMPACT)) {
			storage = new CompactEventStorage(new DefaultVisibility(snId));
		} else if (type.equals(VAL_SIMPLE)) {
			storage = new SimpleEventStorage();
		} else {
			throw new IllegalArgumentException();
		}

		service.setStorage(storage);
	}

	private void configureLogging(SocialNewscastingService service,
			IResolver resolver, String prefix) throws IOException {
		OutputStream log = fManager.get(resolver, prefix);
		if (log != null) {
			LoggingObserver observer = new LoggingObserver(log, false);			
			service.addSubscriber(observer);
		}
	}

	private IReference<IPeerSelector> selector(String prefix) {
		return new ProtocolReference<IPeerSelector>(Configuration.getPid(prefix
				+ "." + prefix + "." + PAR_SELECTOR));
	}

	private IReference<ISelectionFilter> filter(String prefix) {
		String filterName = prefix + "." + prefix + "." + PAR_FILTER;
		if (!Configuration.contains(filterName)) {
			return new FallThroughReference<ISelectionFilter>(
					ISelectionFilter.ALWAYS_TRUE_FILTER);
		}

		return new ProtocolReference<ISelectionFilter>(
				Configuration.getPid(filterName));
	}

	public Object clone() {
		return this;
	}
}
