package it.unitn.disi.simulator.concurrent;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.IResolver;
import it.unitn.disi.distsim.scheduler.ISchedulerClient;
import it.unitn.disi.distsim.scheduler.generators.ISchedule;
import it.unitn.disi.distsim.scheduler.generators.IScheduleIterator;
import it.unitn.disi.distsim.scheduler.generators.Schedulers;
import it.unitn.disi.distsim.scheduler.generators.Schedulers.SchedulerType;

@AutoConfig
public class LocalScheduler implements ISchedulerClient {

	private final ISchedule fSchedule;

	public LocalScheduler(@Attribute("scheduler_type") String type,
			@Attribute(Attribute.AUTO) IResolver resolver) {
		fSchedule = Schedulers.get(
				SchedulerType.valueOf(type.trim().toUpperCase()), resolver);
	}

	@Override
	public IScheduleIterator iterator() {
		return fSchedule.iterator();
	}

	@Override
	public int size() {
		return fSchedule.size();
	}
}
