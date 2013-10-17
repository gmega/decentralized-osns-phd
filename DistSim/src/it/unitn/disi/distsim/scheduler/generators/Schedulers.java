package it.unitn.disi.distsim.scheduler.generators;

import it.unitn.disi.distsim.scheduler.RemoteSchedulerClient;
import peersim.config.IResolver;
import peersim.config.ObjectCreator;

public class Schedulers {

	public static enum SchedulerType {
		IDLIST(IDListScheduler.class), INTERVAL(IntervalScheduler.class), DISTRIBUTED(
				RemoteSchedulerClient.class);

		public final Class<? extends ISchedule> klass;

		private SchedulerType(Class<? extends ISchedule> klass) {
			this.klass = klass;
		}
	}

	public static ISchedule get(String schedulerType, IResolver resolver) {
		return get(SchedulerType.valueOf(schedulerType.toUpperCase()), resolver);
	}

	public static ISchedule get(SchedulerType schedulerType, IResolver resolver) {
		return ObjectCreator.createInstance(schedulerType.klass, "", resolver);
	}

	public static ISchedule loop(ISchedule inner, int times) {
		return new RepetitionDecorator(inner, times, true);
	}

}
