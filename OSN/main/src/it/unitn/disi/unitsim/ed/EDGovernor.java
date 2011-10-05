package it.unitn.disi.unitsim.ed;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.IResolver;
import peersim.core.Control;
import peersim.edsim.EDProtocol;
import it.unitn.disi.newscasting.experiments.schedulers.IScheduleIterator;
import it.unitn.disi.unitsim.GovernorBase;
import it.unitn.disi.unitsim.IExperimentObserver;
import it.unitn.disi.utils.logging.TabularLogManager;

/**
 * Governor for even-driven experiments. Implements {@link Control} so that
 * experiments can be timed out -- that's the easiest way to do it, given that
 * we cannot tap into the PeerSim event queue if we're not an {@link EDProtocol}
 * .
 * 
 * @author giuliano
 */
@AutoConfig
public class EDGovernor extends
		GovernorBase<IEDUnitExperiment, IExperimentObserver<IEDUnitExperiment>>
		implements IExperimentObserver<IEDUnitExperiment>, Control {
	
	public EDGovernor(@Attribute(Attribute.PREFIX) String prefix,
			@Attribute(Attribute.AUTO) IResolver resolver,
			@Attribute("TabularLogManager") TabularLogManager manager,
			@Attribute("experiment") String experiment) {
		super(prefix, resolver, manager, experiment);

	}

	public void start() {
		scheduleNext();
	}

	@Override
	public void experimentEnd(IEDUnitExperiment experiment) {
		wrapUpExperiment();
		System.err.println("END:" + experiment.getId());
		scheduleNext();
	}

	@Override
	public void experimentStart(IEDUnitExperiment experiment) {
	}

	private void scheduleNext() {
		Integer id = fSchedule.nextIfAvailable();
		fCurrent = null;
		if (id == IScheduleIterator.DONE) {
			return;
		}
		fCurrent = create(fResolver, fPrefix, id);
		fCurrent.initialize();
		fCurrent.addObserver(this);
	}

	@Override
	public boolean execute() {
		if (fCurrent == null) {
			return true;
		}
		
		if (fCurrent.isTimedOut()) {
			System.err.println("-- Experiment " + fCurrent.getId()
					+ " timed out.");
			fCurrent.interruptExperiment();
		}
		
		return false;
	}
}
