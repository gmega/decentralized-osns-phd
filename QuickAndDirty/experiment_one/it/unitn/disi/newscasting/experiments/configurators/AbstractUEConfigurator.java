package it.unitn.disi.newscasting.experiments.configurators;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import it.unitn.disi.ISelectionFilter;
import it.unitn.disi.newscasting.IContentExchangeStrategy;
import it.unitn.disi.newscasting.IPeerSelector;
import it.unitn.disi.newscasting.experiments.DisseminationExperimentGovernor;
import it.unitn.disi.newscasting.experiments.ExperimentStatisticsManager;
import it.unitn.disi.newscasting.experiments.SingleEventStorage;
import it.unitn.disi.newscasting.internal.IApplicationConfigurator;
import it.unitn.disi.newscasting.internal.IEventObserver;
import it.unitn.disi.newscasting.internal.IWritableEventStorage;
import it.unitn.disi.newscasting.internal.SocialNewscastingService;
import it.unitn.disi.utils.TableReader;
import it.unitn.disi.utils.peersim.CachingResolver;
import it.unitn.disi.utils.peersim.FallThroughReference;
import peersim.config.AutoConfig;
import peersim.config.Configuration;
import peersim.config.IResolver;
import peersim.config.MissingParameterException;
import peersim.config.ObjectCreator;
import peersim.config.resolvers.CompositeResolver;
import peersim.config.resolvers.PeerSimResolver;

/**
 * Abstract {@link IApplicationConfigurator} implementation for the unit
 * experiment framework.
 * 
 * @author giuliano
 */
@AutoConfig
public abstract class AbstractUEConfigurator implements
		IApplicationConfigurator {

	// ----------------------------------------------------------------------
	// Parameter keys.
	// ----------------------------------------------------------------------

	public static final String PARAMETER_FILE = "parameters";

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
	protected static IResolver fResolver = CachingResolver
			.cachingResolver(new CompositeResolver(new PeerSimResolver()));

	// ----------------------------------------------------------------------

	public AbstractUEConfigurator() {
	}

	// ----------------------------------------------------------------------

	private void oneShotConfig(String prefix) {
		if (fConfigured) {
			return;
		}

		// And the statistics printer.
		StatisticsPrinter printer = ObjectCreator.createInstance(
				StatisticsPrinter.class, prefix);
		DisseminationExperimentGovernor.addExperimentObserver(printer);

		// And the statistics manager. The printer has to come BEFORE
		// the manager, or the experiment data will be wiped out by the
		// time we have the opportunity to print something.
		DisseminationExperimentGovernor
				.addExperimentObserver(ExperimentStatisticsManager
						.getInstance());

		// And the parameter updaters, if any.
		TableReader reader = tableReader(prefix);
		if (reader != null) {
			registerUpdaters(prefix, reader);
		} else {
			System.err.println("No parameter files specified.");
		}

		fConfigured = true;
	}

	// ----------------------------------------------------------------------

	@Override
	public void configure(SocialNewscastingService app, String prefix,
			int protocolId, int socialNetworkId) throws Exception {

		oneShotConfig(prefix);

		// Application storage.
		app.setStorage(storage(prefix, protocolId, socialNetworkId));

		// Unit experiment exchange strategy.
		IContentExchangeStrategy strategy = strategy(app, prefix, protocolId,
				socialNetworkId);

		// The peer selector.
		IPeerSelector selector = selector(app, prefix, protocolId,
				socialNetworkId);

		// And the selection filter.
		ISelectionFilter filter = filter(app, prefix, protocolId,
				socialNetworkId);

		app.addStrategy(classes(), strategy,
				new FallThroughReference<IPeerSelector>(selector),
				new FallThroughReference<ISelectionFilter>(filter), 1.0);

		if (strategy instanceof IEventObserver) {
			app.addSubscriber((IEventObserver) strategy);
		}

		app.addSubscriber(ExperimentStatisticsManager.getInstance());
	}

	// ----------------------------------------------------------------------

	private TableReader tableReader(String prefix) {
		try {
			String filepath = fResolver.getString(prefix, PARAMETER_FILE);
			System.err.println(AbstractUEConfigurator.class.getName()
					+ ": using parameter file " + filepath);
			return new TableReader(new FileInputStream(new File(filepath)));
		} catch (MissingParameterException ex) {
			return null;
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	// ----------------------------------------------------------------------

	protected IWritableEventStorage storage(String prefix, int protocolId,
			int socialNetworkId) {
		// The default storage class for unit experiments is the single message
		// storage.
		return new SingleEventStorage();
	}

	// ----------------------------------------------------------------------

	/**
	 * @return an {@link IContentExchangeStrategy}, which is to be used with the
	 *         unit experiments.
	 */
	protected abstract IContentExchangeStrategy strategy(
			SocialNewscastingService app, String prefix, int protocolId,
			int socialNetworkId);

	/**
	 * @return the {@link Class}es under which to register the
	 *         {@link IContentExchangeStrategy}.
	 */
	protected abstract Class<? extends IContentExchangeStrategy>[] classes();

	/**
	 * @return the {@link IPeerSelector} to be used with the exchange strategy.
	 */
	protected abstract IPeerSelector selector(SocialNewscastingService app,
			String prefix, int protocolId, int socialNetworkId);

	/**
	 * @return the {@link ISelectionFilter} to be used with the exchange
	 *         strategy.
	 */
	protected abstract ISelectionFilter filter(SocialNewscastingService app,
			String prefix, int protocolId, int socialNetworkId);

	/**
	 * Hook for registering updaters.
	 */
	protected void registerUpdaters(String prefix, TableReader reader) {
	}

	// ----------------------------------------------------------------------

	@Override
	public Object clone() {
		return this;
	}
}
