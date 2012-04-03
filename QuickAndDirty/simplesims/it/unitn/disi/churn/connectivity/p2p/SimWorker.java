package it.unitn.disi.churn.connectivity.p2p;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Random;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.IResolver;
import peersim.config.ObjectCreator;

import it.unitn.disi.churn.AssignmentReader;
import it.unitn.disi.churn.GraphConfigurator;
import it.unitn.disi.churn.YaoChurnConfigurator;
import it.unitn.disi.churn.connectivity.SimulationResults;
import it.unitn.disi.churn.connectivity.TEExperimentHelper;
import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.large.catalog.IGraphProvider;

import it.unitn.disi.newscasting.experiments.schedulers.IScheduleIterator;
import it.unitn.disi.newscasting.experiments.schedulers.distributed.DistributedSchedulerClient;
import it.unitn.disi.utils.OrderingUtils;
import it.unitn.disi.utils.streams.ResettableFileInputStream;
import it.unitn.disi.utils.tabular.TableReader;
import it.unitn.disi.utils.tabular.TableWriter;

@AutoConfig
public class SimWorker implements ITransformer {

	@Attribute("sources")
	private String fSources;

	@Attribute("assignments")
	private String fAssignments;

	@Attribute("index")
	private String fAssignmentIndex;

	@Attribute("repeat")
	private int fRepeat;

	@Attribute("cores")
	private int fCores;

	@Attribute("burnin")
	private double fBurnin;

	private double fCloudPercent;

	@Attribute("cloudsims")
	private boolean fCloudSims;

	private ResettableFileInputStream fAssignmentStream;

	private ExperimentEntry[] fIndex;

	private TableReader fSourceReader;

	private AssignmentReader fAssigReader;

	private DistributedSchedulerClient fClient;

	private GraphConfigurator fGraphConfig;

	private YaoChurnConfigurator fYaoConfig;

	private final Random fRandom = new Random();

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
						false, fCloudSims);

				printResults(e.root, results, writer, ids);
			}
		} finally {
			helper.shutdown(true);
		}
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
					Long.parseLong(reader.get("offset"))));
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
		double[][] lidi = readLIDI(root, ids);

		// Reassign the "cloud" nodes by setting their li/dis to negative.
		drawCloud(lidi, root, ids);

		return new Experiment(root, source, lidi[AssignmentReader.LI],
				lidi[AssignmentReader.DI], ids);
	}

	// -------------------------------------------------------------------------

	private void drawCloud(double[][] lidi, int root, int[] ids) {
		int[] nodes = new int[lidi[AssignmentReader.LI].length];
		for (int i = 0; i < nodes.length; i++) {
			nodes[i] = i;
		}

		OrderingUtils.permute(0, nodes.length, nodes, fRandom);
		int cut = (int) Math.ceil(nodes.length * fCloudPercent);
		for (int i = 0; i < cut; i++) {
			System.out.println("CL:" + root + " " + ids[nodes[i]]);
			lidi[AssignmentReader.LI][nodes[i]] = -1;
			lidi[AssignmentReader.DI][nodes[i]] = -1;
		}
	}

	// -------------------------------------------------------------------------

	private void resetReader() throws IOException {
		File f = new File(fSources);
		System.err
				.println("-- Source will be taken from [" + f.getName() + "]");
		fSourceReader = new TableReader(new FileInputStream(f));
	}

	// -------------------------------------------------------------------------

	private double[][] readLIDI(int root, int[] ids) throws IOException {
		int idx = Arrays.binarySearch(fIndex, root);
		if (idx < 0 || idx >= fIndex.length || fIndex[idx].root != root) {
			throw new NoSuchElementException();
		}
		long offset = fIndex[idx].offset;
		fAssignmentStream.reposition(offset);
		fAssigReader.streamRepositioned();
		return fAssigReader.read(ids);
	}

	// -------------------------------------------------------------------------

	static class Experiment {

		final int root;
		final int source;

		final int ids[];

		final double[] lis;
		final double[] dis;

		public Experiment(int root, int source, double[] lis, double[] dis,
				int[] ids) {
			this.root = root;
			this.ids = ids;
			this.source = idOf(source, ids);
			this.lis = lis;
			this.dis = dis;
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
		public final long offset;

		public ExperimentEntry(int root, long offset) {
			this.root = root;
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
