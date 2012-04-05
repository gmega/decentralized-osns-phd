package it.unitn.disi.churn.connectivity.p2p;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.NoSuchElementException;

import it.unitn.disi.churn.AssignmentReader;
import it.unitn.disi.churn.GraphConfigurator;
import it.unitn.disi.churn.YaoChurnConfigurator;
import it.unitn.disi.churn.AssignmentReader.Assignment;
import it.unitn.disi.churn.connectivity.TEExperimentHelper;
import it.unitn.disi.churn.connectivity.p2p.IndexedReader.IndexEntry;
import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.graph.large.catalog.IGraphProvider;
import it.unitn.disi.newscasting.experiments.schedulers.IScheduleIterator;
import it.unitn.disi.newscasting.experiments.schedulers.distributed.DistributedSchedulerClient;
import it.unitn.disi.utils.collections.Pair;

import it.unitn.disi.utils.tabular.TableReader;
import peersim.config.Attribute;
import peersim.config.IResolver;
import peersim.config.ObjectCreator;

/**
 * Base class over which the P2P paper simulation workers are built upon.
 * Provides functions for reading indexed availability assignment files, indexed
 * graphs, initializing the parallel simulators, and reading a row-oriented
 * experiment specification set which is to be shared among all workers.
 * 
 * @author giuliano
 */
public abstract class AbstractWorker implements ITransformer {

	/**
	 * File containing the experiment keys for us to simulate.
	 */
	@Attribute("sources")
	protected String fSources;

	/**
	 * File containing all neighborhood/node assignments.
	 */
	@Attribute("assignments")
	protected String fAssignments;

	/**
	 * Index with the entry position of each neighborhood in the neighborhood/
	 * node assignments file.
	 */
	@Attribute("index")
	protected String fAssignmentIndex;

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

	private IndexedReader fAssigIndex;

	private AssignmentReader fAssigReader;

	private DistributedSchedulerClient fClient;

	protected GraphConfigurator fGraphConfig;

	protected YaoChurnConfigurator fYaoConfig;

	private TEExperimentHelper fHelper;

	private TableReader fSourceReader;

	private String fIdField;

	private String fSourceField;

	// -------------------------------------------------------------------------

	public AbstractWorker(IResolver resolver, String idField, String sourceField) {
		fGraphConfig = ObjectCreator.createInstance(GraphConfigurator.class,
				"", resolver);
		fYaoConfig = ObjectCreator.createInstance(YaoChurnConfigurator.class,
				"", resolver);
		fClient = ObjectCreator.createInstance(
				DistributedSchedulerClient.class, "", resolver);
		fIdField = idField;
		fSourceField = sourceField;
	}

	// -------------------------------------------------------------------------

	protected TEExperimentHelper simHelper() throws Exception {
		if (fHelper == null) {
			fHelper = new TEExperimentHelper(fYaoConfig,
					TEExperimentHelper.EDGE_DISJOINT, fCores, fRepeat, fBurnin);
		}
		return fHelper;
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

	protected Pair<Assignment, IndexEntry> readLIDI(int root, int[] ids)
			throws IOException {
		IndexEntry entry;

		if (fAssigIndex == null) {
			fAssigIndex = IndexedReader.createReader(
					new File(fAssignmentIndex), new File(fAssignments));
			fAssigReader = new AssignmentReader(fAssigIndex.getStream(), "id");
		}

		if ((entry = fAssigIndex.select(root)) == null) {
			throw new NoSuchElementException();
		}

		fAssigReader.streamRepositioned();
		Assignment ass = (Assignment) fAssigReader.read(ids);

		return new Pair<Assignment, IndexEntry>(ass, entry);
	}

	// -------------------------------------------------------------------------

	protected TableReader sourceReader() throws IOException {
		if (fSourceReader == null) {
			resetReader();
		}
		return fSourceReader;
	}

	// -------------------------------------------------------------------------

	private void resetReader() throws IOException {
		File f = new File(fSources);
		System.err
				.println("-- Source will be taken from [" + f.getName() + "]");
		fSourceReader = new TableReader(new FileInputStream(f));
	}

	// -------------------------------------------------------------------------

	protected Experiment readExperiment(Integer row) throws Exception {
		seekSourceRow(row);
		int root = Integer.parseInt(fSourceReader.get(fIdField));
		int source = Integer.parseInt(fSourceReader.get(fSourceField));
		int[] ids = provider().verticesOf(root);

		Pair<Assignment, IndexEntry> assignment = readLIDI(root, ids);

		return new Experiment(root, source, assignment.a.li, assignment.a.di,
				ids, assignment.b);
	}

	// -------------------------------------------------------------------------

	protected void seekSourceRow(Integer row) throws IOException {
		if (row < sourceReader().currentRow()) {
			resetReader();
		}

		while (row > sourceReader().currentRow()) {
			sourceReader().next();
		}
	}

	// -------------------------------------------------------------------------

	protected void shutdown() throws InterruptedException {
		if (fHelper == null) {
			return;
		}
		fHelper.shutdown(true);
	}

	// -------------------------------------------------------------------------

	static class Experiment {

		final int root;
		final int source;

		final int ids[];

		final double[] lis;
		final double[] dis;

		final IndexEntry entry;

		public Experiment(int root, int source, double[] lis, double[] dis,
				int[] ids, IndexEntry entry) {
			this.root = root;
			this.ids = ids;
			this.source = idOf(source, ids);
			this.lis = lis;
			this.dis = dis;
			this.entry = entry;
		}

		private int idOf(int source, int[] ids) {
			for (int i = 0; i < ids.length; i++) {
				if (ids[i] == source) {
					return (i);
				}
			}

			throw new NoSuchElementException(Integer.toString(source));
		}

		public String toString() {
			return "size " + ids.length + ", source " + source;
		}
	}

	// -------------------------------------------------------------------------

}
