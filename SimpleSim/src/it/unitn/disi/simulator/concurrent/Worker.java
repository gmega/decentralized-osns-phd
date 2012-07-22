package it.unitn.disi.simulator.concurrent;

import org.apache.log4j.Logger;

import it.unitn.disi.distsim.scheduler.DistributedSchedulerClient;
import it.unitn.disi.distsim.scheduler.generators.IScheduleIterator;
import it.unitn.disi.utils.tabular.TableWriter;
import peersim.config.Attribute;
import peersim.config.IResolver;
import peersim.config.ObjectCreator;

public class Worker implements Runnable {

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
	
	private TableWriter fOutput;
	
	private String[] fMetrics;

	/**
	 * The iterator which will feed us experiments.
	 */
	private DistributedSchedulerClient fClient;

	public Worker(@Attribute(Attribute.AUTO) IResolver resolver,
			@Attribute("metrics") String[] metrics,
			@Attribute("output") TableWriter output) {
		fClient = ObjectCreator.createInstance(
				DistributedSchedulerClient.class, "scheduler", resolver);
		fMetrics = metrics;
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
			System.out.println(row);
		}
	}

}
