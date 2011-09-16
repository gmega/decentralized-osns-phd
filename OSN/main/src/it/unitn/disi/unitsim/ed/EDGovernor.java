package it.unitn.disi.unitsim.ed;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.IResolver;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.edsim.EDProtocol;
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
	
	private final long fTimeout;

	public EDGovernor(@Attribute(Attribute.PREFIX) String prefix,
			@Attribute(Attribute.AUTO) IResolver resolver,
			@Attribute("TabularLogManager") TabularLogManager manager,
			@Attribute("experiment") String experiment,
			@Attribute(value = "timeout", defaultValue = "0") long timeout) {
		super(prefix, resolver, manager, experiment);
		fTimeout = timeout == 0 ? Long.MAX_VALUE : timeout;
	}

	public void start() {
		scheduleNext();
	}

	@Override
	public void experimentEnd(IEDUnitExperiment experiment) {
		wrapUpExperiment();
		scheduleNext();
		System.err.println("END:" + experiment.getId());
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

	@Override
	public boolean execute() {
		if (fCurrent == null) {
			return false;
		}
		
		if (CommonState.getTime() - fCurrent.startTime() >= fTimeout) {
			fCurrent.interruptExperiment();
		}
		
		return false;
	}
}
