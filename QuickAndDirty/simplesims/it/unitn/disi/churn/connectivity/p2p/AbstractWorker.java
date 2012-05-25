package it.unitn.disi.churn.connectivity.p2p;

import it.unitn.disi.churn.config.ExperimentReader;
import it.unitn.disi.churn.config.GraphConfigurator;
import it.unitn.disi.churn.config.YaoChurnConfigurator;
import it.unitn.disi.churn.connectivity.TEExperimentHelper;
import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.distsim.scheduler.DistributedSchedulerClient;
import it.unitn.disi.distsim.scheduler.generators.IScheduleIterator;
import it.unitn.disi.graph.large.catalog.IGraphProvider;
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
public abstract class AbstractWorker implements ITransformer {

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

	private DistributedSchedulerClient fClient;

	protected GraphConfigurator fGraphConfig;

	protected YaoChurnConfigurator fYaoConfig;

	private TEExperimentHelper fHelper;

	private ExperimentReader fExperimentReader;

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
		fClient = ObjectCreator.createInstance(
				DistributedSchedulerClient.class, "", resolver);

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
		return fGraphConfig.graphProvider();
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
		fHelper.shutdown(true);
	}

	// -------------------------------------------------------------------------

}
