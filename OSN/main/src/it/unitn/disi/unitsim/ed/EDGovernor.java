package it.unitn.disi.unitsim.ed;

import peersim.config.IResolver;
import it.unitn.disi.unitsim.GovernorBase;
import it.unitn.disi.unitsim.IExperimentObserver;
import it.unitn.disi.unitsim.IUnitExperiment;
import it.unitn.disi.utils.logging.TabularLogManager;

public class EDGovernor extends GovernorBase<IEDUnitExperiment> implements IExperimentObserver {

	public EDGovernor(String prefix, IResolver resolver,
			TabularLogManager manager, String experiment) {
		super(prefix, resolver, manager, experiment);
	}
	
	public void start() {
		scheduleNext();
	}

	@Override
	public void experimentEnd(IUnitExperiment experiment) {
		wrapUpExperiment();
		scheduleNext();
	}
	
	@Override
	public void experimentStart(IUnitExperiment experiment) {
	}
	
	private void scheduleNext() {
		if (!fSchedule.hasNext()) {
			return;
		}
		
		Integer id = fSchedule.next();
		fCurrent = create(fResolver, fPrefix, id);
		fCurrent.initialize();
		fCurrent.addObserver(this);
	}
}
