package it.unitn.disi.churn.intersync.markov;

import it.unitn.disi.churn.connectivity.SimulationTaskBuilder;
import it.unitn.disi.churn.connectivity.TEExperimentHelper;
import it.unitn.disi.churn.diffusion.experiments.config.Utils;
import it.unitn.disi.churn.diffusion.graph.BranchingGraphGenerator;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.algorithms.ITopKEstimator;
import it.unitn.disi.graph.algorithms.PathEntry;
import it.unitn.disi.graph.generators.ListGraphGenerator;
import it.unitn.disi.simulator.churnmodel.yao.YaoChurnConfigurator;
import it.unitn.disi.simulator.concurrent.SimulationTask;
import it.unitn.disi.simulator.measure.INodeMetric;
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.collections.Pair;
import it.unitn.disi.utils.logging.IProgressTracker;
import it.unitn.disi.utils.logging.Progress;

import java.util.ArrayList;
import java.util.List;

import org.lambda.functions.implementations.F2;

public class ExoticSimHelper {

	private final int fRepeat;

	private final double fBurnin;

	private final YaoChurnConfigurator fYaoConfig;
	
	private final TEExperimentHelper fHelper;

	public ExoticSimHelper(int repeats, double burnin,
			YaoChurnConfigurator yaoConfig, TEExperimentHelper helper) {
		fRepeat = repeats;
		fBurnin = burnin;
		fYaoConfig = yaoConfig;
		fHelper = helper;
	}

	public double unfoldedGraphSimulation(
			IndexedNeighborGraph graph,
			F2<IndexedNeighborGraph, double[][], ITopKEstimator> estimatorFactory,
			int source, int target, double[][] w, double[] lis, double[] dis,
			int k, boolean coupled) throws Exception{
		ITopKEstimator estimator = estimatorFactory.call(graph, w);

		ArrayList<? extends PathEntry> paths = estimator.topKShortest(source,
				target, k);

		Pair<IndexedNeighborGraph, int[]> result = BranchingGraphGenerator
				.branchingGraph(paths.toArray(new PathEntry[paths.size()]));

		int[] map = result.b;
		IndexedNeighborGraph unfolded = result.a;

		Pair<Integer, int[]> idxMap = coupled ? indexMap(map) : null;

		// Fullsims it:
		List<INodeMetric<Double>> metrics = fHelper
				.bruteForceSimulateMulti(unfolded, 0, 0,
						remap(map, lis),
						remap(map, dis),
						coupled ? idxMap.b : null);

		INodeMetric<Double> metric = Utils.lookup(metrics, "ed", Double.class);
		return metric.getMetric(1);
	}
	
	/**
	 * Index map returns and array with node mappings. If map[i] = j, this means
	 * that nodes i and j are actually the same.
	 * 
	 * @param map
	 * @return
	 */
	private Pair<Integer, int[]> indexMap(final int[] map) {
		int[] idxMap = new int[map.length];
		int remapped = 0;
		for (int i = 0; i < map.length; i++) {
			// Returns the *first index* of map[i].
			int idx = MiscUtils.indexOf(map, map[i]);
			idxMap[i] = idx;
			if (idx != i) {
				System.err.println("Remapped vertex " + i + " to " + idx + ".");
				remapped++;
			}

		}

		return new Pair<Integer, int[]>(remapped, idxMap);
	}

	private double[] remap(int[] map, double[] attribute) {
		double[] mapped = new double[map.length];
		for (int i = 0; i < mapped.length; i++) {
			mapped[i] = attribute[map[i]];
		}
		return mapped;
	}

	public double independentPathSimulation(
			IndexedNeighborGraph graph,
			F2<IndexedNeighborGraph, double[][], ITopKEstimator> estimatorFactory,
			int root, int source, int target, double[][] w, double[] lis,
			double[] dis, int k) throws Exception {

		ITopKEstimator estimator = estimatorFactory.call(graph, w);
		ArrayList<? extends PathEntry> paths = estimator.topKShortest(source,
				target, k);

		IProgressTracker tracker = Progress.newTracker("IP", fRepeat);

		tracker.startTask();

		double sum = 0;
		for (int i = 0; i < fRepeat; i++) {
			double min = Double.MAX_VALUE;
			for (PathEntry entry : paths) {
				min = Math.min(min,
						pathSim(graph, entry.path, source, target, lis, dis));
			}
			tracker.tick();
			sum += min;
		}

		return sum / fRepeat;
	}

	public double pathSim(IndexedNeighborGraph graph, int[] path, int rSource,
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

	public TEExperimentHelper getDelegate() {
		return fHelper;
	}
}
