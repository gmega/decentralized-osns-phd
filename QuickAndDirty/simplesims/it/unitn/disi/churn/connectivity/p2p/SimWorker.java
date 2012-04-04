package it.unitn.disi.churn.connectivity.p2p;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.NoSuchElementException;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.IResolver;
import peersim.config.ObjectCreator;

import gnu.trove.list.array.TIntArrayList;
import it.unitn.disi.churn.AssignmentReader;
import it.unitn.disi.churn.GraphConfigurator;
import it.unitn.disi.churn.YaoChurnConfigurator;
import it.unitn.disi.churn.AssignmentReader.Assignment;
import it.unitn.disi.churn.connectivity.SimulationResults;
import it.unitn.disi.churn.connectivity.TEExperimentHelper;
import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.large.catalog.IGraphProvider;

import it.unitn.disi.newscasting.experiments.schedulers.IScheduleIterator;
import it.unitn.disi.newscasting.experiments.schedulers.distributed.DistributedSchedulerClient;
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.collections.Pair;
import it.unitn.disi.utils.streams.ResettableFileInputStream;
import it.unitn.disi.utils.tabular.TableReader;
import it.unitn.disi.utils.tabular.TableWriter;

@AutoConfig
public class SimWorker implements ITransformer {

	/**
	 * File containing the neighborhood/source pairs for us to simulate.
	 */
	@Attribute("sources")
	private String fSources;

	/**
	 * File containing all neighborhood/node assignments.
	 */
	@Attribute("assignments")
	private String fAssignments;

	/**
	 * Index with the entry position of each neighborhood in the neighborhood/
	 * node assignments file.
	 */
	@Attribute("index")
	private String fAssignmentIndex;

	/**
	 * Bitmap containing which nodes in which neighborhoods are cloud nodes.
	 */
	@Attribute(value = "cloudbitmap", defaultValue = Attribute.VALUE_NULL)
	private String fCloudBitmapFile;

	/**
	 * How many repetitions to run.
	 */
	@Attribute("repeat")
	private int fRepeat;

	@Attribute("cores")
	private int fCores;

	@Attribute("burnin")
	private double fBurnin;

	@Attribute("cloudsims")
	private boolean fCloudSims;

	private ResettableFileInputStream fAssignmentStream;

	private ExperimentEntry[] fIndex;

	private TableReader fSourceReader;

	private AssignmentReader fAssigReader;

	private DistributedSchedulerClient fClient;

	private GraphConfigurator fGraphConfig;

	private YaoChurnConfigurator fYaoConfig;

	private BitSet fCloudBitmap;

	public SimWorker(@Attribute(Attribute.AUTO) IResolver resolver)
			throws IOException {
		fGraphConfig = ObjectCreator.createInstance(GraphConfigurator.class,
				"", resolver);
		fYaoConfig = ObjectCreator.createInstance(YaoChurnConfigurator.class,
				"", resolver);
		fClient = ObjectCreator.createInstance(
				DistributedSchedulerClient.class, "", resolver);
	}

	// -------------------------------------------------------------------------

	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {
		TableWriter writer = new TableWriter(oup, "id", "source", "target",
				"ttc", "ttcloud");

		resetReader();
		fIndex = readIndex();
		fAssignmentStream = new ResettableFileInputStream(
				new File(fAssignments));
		fAssigReader = new AssignmentReader(fAssignmentStream, "id");
		fCloudBitmap = readCloudBitmap();

		IScheduleIterator schedule = fClient.iterator();
		IGraphProvider provider = fGraphConfig.graphProvider();
		TEExperimentHelper helper = new TEExperimentHelper(fYaoConfig,
				TEExperimentHelper.EDGE_DISJOINT, fCores, fRepeat, fBurnin);

		try {
			Integer row;
			while ((row = schedule.nextIfAvailable()) != IScheduleIterator.DONE) {
				Experiment e = readExperiment(row, provider);

				IndexedNeighborGraph graph = provider.subgraph(e.root);
				int[] ids = provider.verticesOf(e.root);

				SimulationResults results = helper.bruteForceSimulate(
						e.toString(), graph, e.source, e.lis, e.dis, ids,
						e.cloudNodes, false, fCloudSims);

				printResults(e.root, results, writer, ids);
			}
		} finally {
			helper.shutdown(true);
		}
	}

	// -------------------------------------------------------------------------

	private BitSet readCloudBitmap() throws Exception {
		ObjectInputStream stream = new ObjectInputStream(new FileInputStream(
				new File(fCloudBitmapFile)));
		return (BitSet) stream.readObject();
	}

	// -------------------------------------------------------------------------

	private ExperimentEntry[] readIndex() throws IOException {
		File f = new File(fAssignmentIndex);

		System.err.println("-- Index -- ");
		System.err.println("- File is " + f.getName() + ".");
		System.err.print("- Reading...");

		TableReader reader = new TableReader(new FileInputStream(f));
		ArrayList<ExperimentEntry> entries = new ArrayList<ExperimentEntry>();
		while (reader.hasNext()) {
			reader.next();
			entries.add(new ExperimentEntry(Integer.parseInt(reader.get("id")),
					Long.parseLong(reader.get("offset")), Integer
							.parseInt(reader.get("row"))));
		}

		System.err.println("done. ");
		System.err.print("- Processing/sorting...");

		ExperimentEntry[] index = entries.toArray(new ExperimentEntry[entries
				.size()]);
		Arrays.sort(index);

		System.err.println("done. ");

		return index;
	}

	// -------------------------------------------------------------------------

	private void printResults(int root, SimulationResults results,
			TableWriter writer, int[] ids) {
		for (int i = 0; i < results.bruteForce.length; i++) {
			if (results.source == i) {
				continue;
			}
			writer.set("id", root);
			writer.set("source", ids[results.source]);
			writer.set("target", ids[i]);
			writer.set("ttc", results.bruteForce[i] / fRepeat);
			writer.set("ttcloud", results.cloud[i] / fRepeat);
			writer.emmitRow();
		}
	}

	// -------------------------------------------------------------------------

	private Experiment readExperiment(Integer row, IGraphProvider provider)
			throws IOException {
		if (row < fSourceReader.currentRow()) {
			resetReader();
		}

		while (row > fSourceReader.currentRow()) {
			fSourceReader.next();
		}

		int root = Integer.parseInt(fSourceReader.get("id"));
		int source = Integer.parseInt(fSourceReader.get("node"));
		int[] ids = provider.verticesOf(root);
		Pair<Assignment, int[]> assignment = readLIDI(root, ids);

		return new Experiment(root, source, assignment.a.li, assignment.a.di,
				ids, assignment.b);
	}

	// -------------------------------------------------------------------------

	private void resetReader() throws IOException {
		File f = new File(fSources);
		System.err
				.println("-- Source will be taken from [" + f.getName() + "]");
		fSourceReader = new TableReader(new FileInputStream(f));
	}

	// -------------------------------------------------------------------------

	private Pair<Assignment, int[]> readLIDI(int root, int[] ids)
			throws IOException {
		// Finds index entry.
		int idx = Arrays.binarySearch(fIndex, root);
		if (idx < 0 || idx >= fIndex.length || fIndex[idx].root != root) {
			throw new NoSuchElementException();
		}

		// Reads assignment.
		long offset = fIndex[idx].offset;
		fAssignmentStream.reposition(offset);
		fAssigReader.streamRepositioned();

		Assignment ass = (Assignment) fAssigReader.read(ids);

		// Reads cloud nodes.
		TIntArrayList cloud = new TIntArrayList();
		for (int i = 0; i < ids.length; i++) {
			int row = fIndex[idx].rowStart + i;
			if (fCloudBitmap.get(row)) {
				cloud.add(ass.nodes[i]);
			}
		}

		return new Pair<Assignment, int[]>(ass, cloud.toArray());
	}

	// -------------------------------------------------------------------------

	static class Experiment {

		final int root;
		final int source;

		final int ids[];

		final int[] cloudNodes;

		final double[] lis;
		final double[] dis;

		public Experiment(int root, int source, double[] lis, double[] dis,
				int[] ids, int[] cloudNodes) {
			this.root = root;
			this.ids = ids;
			this.source = idOf(source, ids);
			this.lis = lis;
			this.dis = dis;
			this.cloudNodes = cloudNodes;
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

	private static class ExperimentEntry implements Comparable<Object> {

		public final int root;
		public final int rowStart;
		public final long offset;

		public ExperimentEntry(int root, long offset, int rowStart) {
			this.root = root;
			this.rowStart = rowStart;
			this.offset = offset;
		}

		@Override
		public int compareTo(Object o) {
			if (o instanceof Integer) {
				return compareInteger((Integer) o);
			} else {
				return compareEntry((ExperimentEntry) o);
			}
		}

		private int compareEntry(ExperimentEntry o) {
			return this.root - o.root;
		}

		private int compareInteger(Integer o) {
			return this.root - o;
		}
	}

	// -------------------------------------------------------------------------
}
