package it.unitn.disi.churn.connectivity.p2p;

import it.unitn.disi.churn.config.ExperimentReader;
import it.unitn.disi.churn.config.GraphConfigurator;
import it.unitn.disi.churn.connectivity.TEExperimentHelper;
import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.distsim.control.ControlClient;
import it.unitn.disi.distsim.scheduler.IWorker;
import it.unitn.disi.distsim.scheduler.RemoteSchedulerClient;
import it.unitn.disi.distsim.scheduler.generators.IScheduleIterator;
import it.unitn.disi.graph.IGraphProvider;
import it.unitn.disi.simulator.churnmodel.yao.YaoChurnConfigurator;

import java.util.Properties;

import peersim.config.Attribute;
import peersim.config.IResolver;
import peersim.config.ObjectCreator;

/**
 * Base class over which the P2P paper simulation workers are built upon.
 * Provides functions for reading indexed availability assignment files, indexed
 * graphs, initializing the parallel simulators, and reading a row-oriented
 * experiment specification file which is to be shared among all workers.
 * 
 * @author giuliano
 */
public abstract class AbstractWorker implements ITransformer, IWorker {

	/**
	 * How many repetitions to run.
	 */
	@Attribute("repetitions")
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
	protected double fBurnin;

	private RemoteSchedulerClient fClient;

	protected GraphConfigurator fGraphConfig;

	protected YaoChurnConfigurator fYaoConfig;

	private TEExperimentHelper fHelper;

	private ExperimentReader fExperimentReader;

	private IGraphProvider fProvider;

	// -------------------------------------------------------------------------

	/**
	 * Constructor for {@link AbstractWorker}.
	 * 
	 * @param resolver
	 *            the resolver for configuring this worker.
	 * @param idField
	 *            the field identifying the ego network in the experiment
	 *            specification file.
	 */
	public AbstractWorker(IResolver resolver, String idField) {
		fGraphConfig = ObjectCreator.createInstance(GraphConfigurator.class,
				"", resolver);
		fYaoConfig = ObjectCreator.createInstance(YaoChurnConfigurator.class,
				"", resolver);

		ControlClient cclient = ObjectCreator.createInstance(
				ControlClient.class, "", resolver);

		fClient = new RemoteSchedulerClient(cclient, this);

		fExperimentReader = new ExperimentReader(idField);
		ObjectCreator.fieldInject(ExperimentReader.class, fExperimentReader,
				"", resolver);
	}

	// -------------------------------------------------------------------------

	protected TEExperimentHelper simHelper() throws Exception {
		if (fHelper == null) {
			fHelper = new TEExperimentHelper(fYaoConfig, fCores, fRepeat,
					fBurnin);
		}
		return fHelper;
	}

	// -------------------------------------------------------------------------

	protected ExperimentReader experimentReader() {
		return fExperimentReader;
	}

	// -------------------------------------------------------------------------

	protected IGraphProvider provider() throws Exception {
		if (fProvider == null) {
			fProvider = fGraphConfig.graphProvider();
		}
		return fProvider;
	}

	// -------------------------------------------------------------------------

	protected IScheduleIterator iterator() throws Exception {
		return fClient.iterator();
	}

	// -------------------------------------------------------------------------

	protected void shutdown() throws InterruptedException {
		if (fHelper == null) {
			return;
		}
		fHelper.shutdown(false);
	}

	// -------------------------------------------------------------------------

	public void echo() {

	}

	// -------------------------------------------------------------------------
	
	public Properties status() {
		return new Properties();
	}

	// -------------------------------------------------------------------------

}
