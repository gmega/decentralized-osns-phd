package it.unitn.disi.simulator.concurrent;

import org.apache.log4j.Logger;

import it.unitn.disi.distsim.scheduler.DistributedSchedulerClient;
import it.unitn.disi.distsim.scheduler.generators.IScheduleIterator;
import it.unitn.disi.simulator.core.EDSimulationEngine;
import it.unitn.disi.simulator.measure.MetricsCollector;
import peersim.config.Attribute;
import peersim.config.IResolver;
import peersim.config.ObjectCreator;

public abstract class Worker implements Runnable {

	private static final Logger fLogger = Logger.getLogger(Worker.class);

	/**
	 * How many repetitions to run.
	 */
	@Attribute("repeat")
	protected int fRepeat;

	/**
	 * How many cores to use.
	 */
	@Attribute("cores")
	protected int fCores;

	/**
	 * What burnin value to use.
	 */
	@Attribute("burnin")
	private double fBurnin;

	/**
	 * The iterator which will feed us experiments.
	 */
	private DistributedSchedulerClient fClient;

	public Worker(@Attribute(Attribute.AUTO) IResolver resolver) {
		fClient = ObjectCreator.createInstance(
				DistributedSchedulerClient.class, "scheduler", resolver);
	}

	@Override
	public void run() {
		try {
			this.run0();
		} catch (Exception ex) {
			fLogger.error(ex);
		}
	}

	private void run0() throws Exception {
		IScheduleIterator schedule = fClient.iterator();
		Integer row;
		while ((row = (Integer) schedule.nextIfAvailable()) != IScheduleIterator.DONE) {
			MetricsCollector collector = runTasks(row);
			
		}
	}

	private MetricsCollector runTasks(Integer row) {
		return null;
	}

	static class SimulationState {
		public final int completedTasks;
		public final MetricsCollector result;

		public SimulationState(int completedTasks, MetricsCollector result) {
			this.completedTasks = completedTasks;
			this.result = result;
		}
	}
}
