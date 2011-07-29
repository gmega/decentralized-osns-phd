package it.unitn.disi.newscasting.experiments.configurators;

import it.unitn.disi.epidemics.CachingConfigurator;
import it.unitn.disi.epidemics.IApplicationConfigurator;
import it.unitn.disi.epidemics.IContentExchangeStrategy;
import it.unitn.disi.epidemics.IEventObserver;
import it.unitn.disi.epidemics.IPeerSelector;
import it.unitn.disi.epidemics.IProtocolSet;
import it.unitn.disi.epidemics.ISelectionFilter;
import it.unitn.disi.epidemics.IWritableEventStorage;
import it.unitn.disi.newscasting.experiments.ExperimentStatisticsManager;
import it.unitn.disi.newscasting.experiments.SelectionFailureTracker;
import it.unitn.disi.newscasting.experiments.SingleEventStorage;
import it.unitn.disi.newscasting.experiments.churn.TimeoutReset;
import it.unitn.disi.newscasting.internal.SocialNewscastingService;
import it.unitn.disi.unitsim.CDGovernor;
import it.unitn.disi.utils.TableReader;
import it.unitn.disi.utils.peersim.FallThroughReference;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import peersim.config.AutoConfig;
import peersim.config.IResolver;
import peersim.config.MissingParameterException;
import peersim.config.ObjectCreator;

/**
 * Abstract {@link IApplicationConfigurator} implementation for the unit
 * experiment framework.
 * 
 * @author giuliano
 */
@AutoConfig
public abstract class AbstractUEConfigurator extends CachingConfigurator {

	// ----------------------------------------------------------------------
	// Parameter keys.
	// ----------------------------------------------------------------------

	public static final String PARAMETER_FILE = "parameters";

	public static final String PARAMETER_TIMEOUT_ID = "timeout_id";

	public static final String PAR_TRACK_SELECT_FAILURES = "selection_failures";

	// ----------------------------------------------------------------------
	
	protected static CDGovernor fGovernor;

	public AbstractUEConfigurator() {
	}

	// ----------------------------------------------------------------------

	protected void oneShotConfig(String prefix, IResolver resolver) {
		// And the statistics printer.
		StatisticsPrinter printer = ObjectCreator.createInstance(
				StatisticsPrinter.class, prefix, resolver);
		
		fGovernor = (CDGovernor) resolver.getObject(
				IResolver.NULL_KEY, CDGovernor.class.getSimpleName());
		
		fGovernor.addExperimentObserver(printer);

		// And the statistics manager. The printer has to come BEFORE
		// the manager, or the experiment data will be wiped out by the
		// time we have the opportunity to print something.
		fGovernor.addExperimentObserver(ExperimentStatisticsManager.getInstance());

		// And the parameter updaters, if any.
		TableReader reader = tableReader(prefix);
		if (reader != null) {
			registerUpdaters(prefix, reader);
		} else {
			System.err.println("No parameter files specified.");
		}
	}

	// ----------------------------------------------------------------------

	public void configure0(IProtocolSet set, IResolver resolver, String prefix)
			throws Exception {

		SocialNewscastingService app = (SocialNewscastingService) set;
		int protocolId = app.pid();
		int socialNetworkId = app.socialNetworkId();

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

		if (fResolver.getBoolean(prefix, PAR_TRACK_SELECT_FAILURES)) {
			filter = new SelectionFailureTracker(
					new FallThroughReference<ISelectionFilter>(filter));
		}

		app.addStrategy(classes(), strategy,
				new FallThroughReference<IPeerSelector>(selector),
				new FallThroughReference<ISelectionFilter>(filter));

		if (strategy instanceof IEventObserver) {
			app.addSubscriber((IEventObserver) strategy);
		}

		// And the timeout controller, if installed.
		try {
			int pid = fResolver.getInt(prefix, PARAMETER_TIMEOUT_ID);
			TimeoutReset resetter = new TimeoutReset(pid);
			app.addSubscriber(resetter);
		} catch (MissingParameterException ex) {
			// Swallows and proceeds.
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
