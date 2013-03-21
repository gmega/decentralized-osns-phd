package it.unitn.disi.churn.intersync.markov;

import it.unitn.disi.churn.config.Experiment;
import it.unitn.disi.churn.config.IndexedReader;
import it.unitn.disi.churn.config.MatrixReader;
import it.unitn.disi.churn.connectivity.SimulationTaskBuilder;
import it.unitn.disi.churn.connectivity.TEExperimentHelper;
import it.unitn.disi.churn.connectivity.p2p.AbstractWorker;
import it.unitn.disi.churn.diffusion.experiments.config.Utils;
import it.unitn.disi.distsim.scheduler.generators.IScheduleIterator;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.analysis.ITopKEstimator;
import it.unitn.disi.graph.analysis.PathEntry;
import it.unitn.disi.newscasting.experiments.schedulers.SchedulerFactory;
import it.unitn.disi.simulator.concurrent.SimulationTask;
import it.unitn.disi.unitsim.ListGraphGenerator;
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.logging.IProgressTracker;
import it.unitn.disi.utils.logging.Progress;
import it.unitn.disi.utils.streams.PrefixedWriter;
import it.unitn.disi.utils.tabular.TableWriter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.NoSuchElementException;

import org.lambda.functions.implementations.F2;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.IResolver;

@AutoConfig
public class TopKGraphAverages extends AbstractWorker {

	@Attribute("weights")
	private String fWeightDb;

	@Attribute("weight-index")
	private String fWeightIdx;

	@Attribute(value = "maxsize")
	private long fMaxSize;

	@Attribute("k")
	private int fK;

	@Attribute(value = "simulate", defaultValue = "true")
	private boolean fSimulate;

	@Attribute(value = "independent", defaultValue = "false")
	private boolean fSimulateIndependent;

	@Attribute(value = "scheduler.type", defaultValue = "default")
	private String fIterator;

	private IResolver fResolver;

	public TopKGraphAverages(@Attribute(Attribute.AUTO) IResolver resolver,
			@Attribute("mode") String mode) throws IOException {
		super(resolver, "id");
		fResolver = resolver;
	}

	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {
		IndexedReader reader = IndexedReader.createReader(new File(fWeightIdx),
				new File(fWeightDb));

		MatrixReader wReader = new MatrixReader(reader.getStream(), "id",
				"source", "target", "delay");

		TableWriter writer = new TableWriter(new PrefixedWriter("ES:", oup),
				"id", "source", "target", "mdelay", "sdelay", "ipdelay");

		ExoticSimHelper helper = new ExoticSimHelper(fRepeat, fBurnin,
				fYaoConfig, simHelper());

		System.out.println("Maximum matrix elements: "
				+ MarkovDelayModel.count(fMaxSize) + ".");

		IScheduleIterator iterator = iterator();
		Integer row;
		while ((row = (Integer) iterator.nextIfAvailable()) != IScheduleIterator.DONE) {
			// Reads availabilities.
			Experiment exp = experimentReader().readExperiment(row, provider());
			IndexedNeighborGraph graph = provider().subgraph(exp.root);
			int[] ids = provider().verticesOf(exp.root);

			int target = Integer.parseInt(exp.attributes.get("target"));
			int source = Integer.parseInt(exp.attributes.get("source"));

			int rTarget = MiscUtils.indexOf(ids, target);
			int rSource = MiscUtils.indexOf(ids, source);

			// Reads weights.
			if (reader.select(exp.root) == null) {
				throw new NoSuchElementException();
			}

			wReader.streamRepositioned();
			double[][] w = wReader.read(ids);

			double simDelay = -1;
			double indDelay = -1;

			if (fSimulate) {
				simDelay = helper.unfoldedGraphSimulation(graph,
						TEExperimentHelper.VERTEX_DISJOINT, rSource, rTarget,
						w, exp.lis, exp.dis, fK, true);
			}

			if (fSimulateIndependent) {
				indDelay = independentPathSimulation(graph,
						TEExperimentHelper.VERTEX_DISJOINT, exp.root, rSource,
						rTarget, w, exp.lis, exp.dis, fK, ids);
			}

			MarkovDelayModel mdm = new MarkovDelayModel(graph,
					lambdaUp(exp.lis), lambdaDown(exp.dis), fMaxSize);
			double modelDelay = mdm.estimateDelay(rSource, rTarget, fK);

			writer.set("id", exp.root);
			writer.set("source", source);
			writer.set("target", target);
			writer.set("sdelay", simDelay);
			writer.set("mdelay", modelDelay);
			writer.set("ipdelay", indDelay);
			writer.emmitRow();
		}
	}

	@Override
	protected IScheduleIterator iterator() throws Exception {
		if (fIterator.equals("default")) {
			return super.iterator();
		}
		return SchedulerFactory.getInstance().createScheduler(fResolver, "")
				.iterator();
	}

	private double[] lambdaDown(double[] dis) {
		double[] gammas = new double[dis.length];
		for (int i = 0; i < gammas.length; i++) {
			gammas[i] = 2.0 / dis[i];
		}
		return gammas;
	}

	private double[] lambdaUp(double[] lis) {
		double[] mus = new double[lis.length];
		for (int i = 0; i < mus.length; i++) {
			mus[i] = 1.0 / lis[i];
		}
		return mus;
	}

	private double independentPathSimulation(
			IndexedNeighborGraph graph,
			F2<IndexedNeighborGraph, double[][], ITopKEstimator> vertexDisjoint,
			int root, int rSource, int rTarget, double[][] w, double[] lis,
			double[] dis, int k, int[] ids) throws Exception {

		ITopKEstimator estimator = vertexDisjoint.call(graph, w);
		ArrayList<? extends PathEntry> paths = estimator.topKShortest(rSource,
				rTarget, k);

		IProgressTracker tracker = Progress.newTracker("IP", fRepeat);

		tracker.startTask();

		double sum = 0;
		for (int i = 0; i < fRepeat; i++) {
			double min = Double.MAX_VALUE;
			for (PathEntry entry : paths) {
				min = Math.min(min,
						pathSim(graph, entry.path, rSource, rTarget, lis, dis));
			}
			tracker.tick();
			sum += min;
		}

		return sum / fRepeat;
	}

	private double pathSim(IndexedNeighborGraph graph, int[] path, int rSource,
			int rTarget, double[] lis, double[] dis) throws Exception {

		ListGraphGenerator lgg = new ListGraphGenerator();
		IndexedNeighborGraph pathGraph = lgg.subgraph(path.length);

		double[] pathLIs = new double[pathGraph.size()];
		double[] pathDIs = new double[pathGraph.size()];

		for (int i = 0; i < pathLIs.length; i++) {
			pathLIs[i] = lis[path[i]];
			pathDIs[i] = dis[path[i]];
		}

		SimulationTaskBuilder builder = new SimulationTaskBuilder(pathGraph, 0,
				pathLIs, pathDIs, fYaoConfig);

		builder.addConnectivitySimulation(0, null, null, "ed", "rd");

		SimulationTask task = builder.simulationTask(fBurnin);
		task.call();

		return Utils.lookup(task.metric(0), "ed", Double.class).getMetric(
				path.length - 1);
	}
}
