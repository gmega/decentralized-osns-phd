package it.unitn.disi.utils.peersim;

import peersim.config.AutoConfig;
import peersim.core.Node;
import peersim.core.Protocol;
import peersim.edsim.EDSimulator;

/**
 * {@link GenericScheduler} allows scheduling of events both in cycle and
 * event-driven modes.
 * <BR>
 * Polymorphic shell on top of PeerSim's singleton-heavy design.
 * 
 * @author giuliano
 */
@AutoConfig
public class GenericScheduler implements IScheduler<Object>, Protocol {

	@Override
	public void schedule(long time, int pid, Node source, Object event) {
		if (EDSimulator.isConfigurationEventDriven()) {
			EDSimulator.add(time, event, source, pid);
		} else {
			CDActionScheduler.add(time, event, source, pid);
		}
	}
	
	@Override
	public Object clone() {
		return this;
	}

}