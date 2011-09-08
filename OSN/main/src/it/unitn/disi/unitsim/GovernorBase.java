package it.unitn.disi.unitsim;

import it.unitn.disi.newscasting.experiments.schedulers.IScheduleIterator;
import it.unitn.disi.newscasting.experiments.schedulers.SchedulerFactory;
import it.unitn.disi.unitsim.cd.ICDExperimentObserver;
import it.unitn.disi.utils.HashMapResolver;
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.logging.TabularLogManager;
import it.unitn.disi.utils.peersim.NodeRegistry;

import java.util.HashMap;
import java.util.Vector;

import peersim.config.Attribute;
import peersim.config.IResolver;
import peersim.config.ObjectCreator;
import peersim.config.plugin.IPlugin;
import peersim.config.resolvers.CompositeResolver;

public abstract class GovernorBase<T extends IUnitExperiment> implements IPlugin{

	protected static final String SCHEDULER = "scheduler";
	/**
	 * Attributes which we append to the configuration data.
	 */
	protected HashMap<String, Object> fAttributes;
	/**
	 * {@link IResolver} used for configuration.
	 */
	protected final IResolver fResolver;
	/**
	 * Our configuration prefix.
	 */
	protected final String fPrefix;
	/**
	 * Registered {@link ICDExperimentObserver}s.
	 */
	protected final Vector<ICDExperimentObserver> fObservers = new Vector<ICDExperimentObserver>();
	/**
	 * The experiment schedule.
	 */
	protected IScheduleIterator fSchedule;
	/**
	 * The currently running experiment.
	 */
	protected T fCurrent;
	protected final TimeTracker fTracker;
	protected final Class<T> fExperimentKlass;

	@SuppressWarnings("unchecked")
	public GovernorBase(@Attribute(Attribute.PREFIX) String prefix,
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
			fExperimentKlass = (Class<T>) Class.forName(experiment);
		} catch (Exception ex) {
			throw MiscUtils.nestRuntimeException(ex);
		}
	}

	protected T create(IResolver resolver, String prefix, Integer id) {
		fAttributes.put(IUnitExperiment.ID, Integer.toString(id));
		return ObjectCreator.createInstance(fExperimentKlass, prefix, resolver);
	}

	protected void wrapUpExperiment() {
		// Notifies the observers before cleaning any state.
		for (ICDExperimentObserver observer : fObservers) {
			observer.experimentEnd(fCurrent);
		}
	
		fCurrent.done();
	}

	public void addExperimentObserver(ICDExperimentObserver observer) {
		fObservers.add(observer);
	}

	public long experimentTime() {
		return fTracker.experimentTime();
	}

	public IUnitExperiment currentExperiment() {
		return fCurrent;
	}

	@Override
	public String id() {
		return this.getClass().getSimpleName();
	}

	@Override
	public void start(IResolver resolver) throws Exception {
	}

	@Override
	public void stop() throws Exception {
	}

}