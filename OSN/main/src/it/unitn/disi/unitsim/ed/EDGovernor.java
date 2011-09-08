package it.unitn.disi.unitsim.ed;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.IResolver;
import it.unitn.disi.unitsim.GovernorBase;
import it.unitn.disi.unitsim.IExperimentObserver;
import it.unitn.disi.unitsim.IUnitExperiment;
import it.unitn.disi.utils.logging.TabularLogManager;

@AutoConfig
public class EDGovernor extends
		GovernorBase<IEDUnitExperiment, IExperimentObserver<IEDUnitExperiment>>
		implements IExperimentObserver<IEDUnitExperiment> {

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
		scheduleNext();
	}
	
	@Override
	public void experimentStart(IEDUnitExperiment experiment) {
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
