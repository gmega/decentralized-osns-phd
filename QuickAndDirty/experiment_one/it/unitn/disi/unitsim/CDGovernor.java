package it.unitn.disi.unitsim;

import it.unitn.disi.newscasting.experiments.TimeTracker;
import it.unitn.disi.newscasting.experiments.schedulers.IScheduleIterator;
import it.unitn.disi.newscasting.experiments.schedulers.SchedulerFactory;
import it.unitn.disi.utils.HashMapResolver;
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.logging.TabularLogManager;
import it.unitn.disi.utils.peersim.NodeRegistry;

import java.util.HashMap;
import java.util.Vector;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.IResolver;
import peersim.config.ObjectCreator;
import peersim.config.plugin.IPlugin;
import peersim.config.resolvers.CompositeResolver;
import peersim.core.Control;

/**
 * Generic, cycle-driven experiment governor for the unit experiment framework.
 * 
 * @author giuliano
 */
@AutoConfig
public class CDGovernor implements Control, IPlugin {

	// ------------------------------------------------------------------------
	// Parameter constants.
	// ------------------------------------------------------------------------

	private static final String SCHEDULER = "scheduler";

	/**
	 * The governor can be in one of three states.
	 * 
	 * @author giuliano
	 */
	static enum SchedulingState {
		/**
		 * Governor is currently running a unit experiment.
		 */
		RUN,

		/**
		 * Governor is currently trying to schedule a unit experiment, possibly
		 * waiting until certain conditions are met by the network.
		 */
		SCHEDULE,

		/**
		 * Governor has completed the experiment schedule.
		 */
		DONE
	}

	// ------------------------------------------------------------------------

	/**
	 * Attributes which we append to the configuration data.
	 */
	private HashMap<String, Object> fAttributes;

	/**
	 * {@link IResolver} used for configuration.
	 */
	private final IResolver fResolver;

	/**
	 * Our configuration prefix.
	 */
	private final String fPrefix;

	/**
	 * Registered {@link ICDExperimentObserver}s.
	 */
	private final Vector<ICDExperimentObserver> fObservers = new Vector<ICDExperimentObserver>();

	/**
	 * The current state for the governor (see {@link SchedulingState}).
	 */
	private SchedulingState fState = SchedulingState.SCHEDULE;

	/**
	 * The experiment schedule.
	 */
	private IScheduleIterator fSchedule;

	/**
	 * The currently running experiment.
	 */
	private ICDUnitExperiment fCurrent;

	private final TimeTracker fTracker;

	private final Class<? extends ICDUnitExperiment> fExperimentKlass;

	// ------------------------------------------------------------------------

	@SuppressWarnings("unchecked")
	public CDGovernor(@Attribute(Attribute.PREFIX) String prefix,
			@Attribute(Attribute.AUTO) IResolver resolver,
			@Attribute("TabularLogManager") TabularLogManager manager,
			@Attribute("experiment") String experiment) {

		fAttributes = new HashMap<String, Object>();
		CompositeResolver composite = new CompositeResolver();
		composite.addResolver(new HashMapResolver(fAttributes));
		composite.addResolver(resolver);
		fResolver = composite.asResolver();

		fSchedule = (IScheduleIterator) SchedulerFactory
				.getInstance()
				.createScheduler(fResolver, prefix + "." + SCHEDULER,
						NodeRegistry.getInstance()).iterator();

		fPrefix = prefix;
		fTracker = new TimeTracker(fSchedule.remaining(), manager);
		addExperimentObserver(fTracker);

		try {
			fExperimentKlass = (Class<? extends ICDUnitExperiment>) Class
					.forName(experiment);
		} catch (Exception ex) {
			throw MiscUtils.nestRuntimeException(ex);
		}
	}

	// ------------------------------------------------------------------------

	@Override
	public boolean execute() {
		switch (fState) {

		case SCHEDULE:
			fState = scheduleCycle();
			break;

		case RUN:
			fState = runCycle();
			break;

		case DONE:
			break;
		}

		return fState == SchedulingState.DONE;
	}

	// ------------------------------------------------------------------------

	private SchedulingState scheduleCycle() {

		if (!fSchedule.hasNext()) {
			return SchedulingState.DONE;
		}

		Integer id = fSchedule.next();
		if (id == null) {
			return SchedulingState.SCHEDULE;
		}

		fCurrent = create(fResolver, fPrefix, id);
		fCurrent.initialize();

		// Condenses a run cycle here.
		runCycle();

		return SchedulingState.RUN;
	}

	// ------------------------------------------------------------------------

	private ICDUnitExperiment create(IResolver resolver, String prefix,
			Integer id) {
		fAttributes.put(ICDUnitExperiment.ID, Integer.toString(id));
		return ObjectCreator.createInstance(fExperimentKlass, prefix, resolver);
	}

	// ------------------------------------------------------------------------

	private SchedulingState runCycle() {
		if (fCurrent.isOver()) {
			// Runs post-unit-experiment code.
			wrapUpExperiment();
			// Schedules the next one, if any.
			return SchedulingState.SCHEDULE;
		}

		for (ICDExperimentObserver observer : fObservers) {
			observer.experimentCycled(fCurrent);
		}

		return SchedulingState.RUN;
	}

	// ------------------------------------------------------------------------

	private void wrapUpExperiment() {
		// Notifies the observers before cleaning any state.
		for (ICDExperimentObserver observer : fObservers) {
			observer.experimentEnd(fCurrent);
		}

		fCurrent.done();
	}

	// ------------------------------------------------------------------------

	public void addExperimentObserver(ICDExperimentObserver observer) {
		fObservers.add(observer);
	}

	// ------------------------------------------------------------------------

	public long experimentTime() {
		return fTracker.experimentTime();
	}

	// ------------------------------------------------------------------------

	public ICDUnitExperiment currentExperiment() {
		return fCurrent;
	}

	// ------------------------------------------------------------------------
	// IPlugin interface.
	// ------------------------------------------------------------------------

	@Override
	public String id() {
		return CDGovernor.class.getSimpleName();
	}

	// ------------------------------------------------------------------------

	@Override
	public void start(IResolver resolver) throws Exception {
	}

	// ------------------------------------------------------------------------

	@Override
	public void stop() throws Exception {
	}
}
