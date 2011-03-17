package it.unitn.disi.application;

import peersim.core.Node;
import peersim.edsim.EDSimulator;

/**
 * {@link GenericScheduler} allows scheduling of events both in cycle and
 * event-driven modes.
 * <BR>
 * Polymorphic shell on top of PeerSim's singleton-heavy design.
 * 
 * @author giuliano
 */
public class GenericScheduler implements IScheduler<Object> {

	@Override
	public void schedule(long time, int pid, Node source, Object event) {
		if (EDSimulator.isConfigurationEventDriven()) {
			EDSimulator.add(time, event, source, pid);
		} else {
			CDActionScheduler.add(time, event, source, pid);
		}
	}

}