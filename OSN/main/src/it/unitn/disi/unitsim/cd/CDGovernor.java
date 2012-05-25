package it.unitn.disi.unitsim.cd;

import it.unitn.disi.distsim.scheduler.generators.IScheduleIterator;
import it.unitn.disi.unitsim.GovernorBase;
import it.unitn.disi.utils.logging.TabularLogManager;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.IResolver;
import peersim.core.Control;

/**
 * Generic, cycle-driven experiment governor for the unit experiment framework.
 * 
 * @author giuliano
 */
@AutoConfig
public class CDGovernor extends
		GovernorBase<ICDUnitExperiment, ICDExperimentObserver> implements
		Control {

	// ------------------------------------------------------------------------
	// Parameter constants.
	// ------------------------------------------------------------------------

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
	 * The current state for the governor (see {@link SchedulingState}).
	 */
	private SchedulingState fState = SchedulingState.SCHEDULE;

	public CDGovernor(@Attribute(Attribute.PREFIX) String prefix,
			@Attribute(Attribute.AUTO) IResolver resolver,
			@Attribute("TabularLogManager") TabularLogManager manager,
			@Attribute("experiment") String experiment) {
		super(prefix, resolver, manager, experiment);
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

		Integer id = (Integer) fSchedule.nextIfAvailable();
		if (id == null) {
			return SchedulingState.SCHEDULE;
		} else if (id == IScheduleIterator.DONE) {
			return SchedulingState.DONE;
		}

		fCurrent = create(fResolver, fPrefix, id);
		fCurrent.initialize();

		currentExperimentStarted();

		// Condenses a run cycle here.
		return runCycle();
	}

	// ------------------------------------------------------------------------

	private SchedulingState runCycle() {
		if (fCurrent.cycled()) {
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
}
