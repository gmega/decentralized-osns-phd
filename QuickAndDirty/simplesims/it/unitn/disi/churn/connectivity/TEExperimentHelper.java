package it.unitn.disi.churn.connectivity;

import it.unitn.disi.churn.connectivity.tce.CloudSim;
import it.unitn.disi.churn.diffusion.experiments.config.Utils;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.algorithms.DunnTopK;
import it.unitn.disi.graph.algorithms.DunnTopK.Mode;
import it.unitn.disi.graph.algorithms.GraphAlgorithms;
import it.unitn.disi.graph.algorithms.ITopKEstimator;
import it.unitn.disi.graph.algorithms.LawlerTopK;
import it.unitn.disi.graph.algorithms.PathEntry;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;
import it.unitn.disi.simulator.churnmodel.yao.YaoChurnConfigurator;
import it.unitn.disi.simulator.concurrent.SimulationTask;
import it.unitn.disi.simulator.concurrent.TaskExecutor;
import it.unitn.disi.simulator.measure.AvgAccumulation;
import it.unitn.disi.simulator.measure.AvgEvaluator;
import it.unitn.disi.simulator.measure.INodeMetric;
import it.unitn.disi.utils.collections.Pair;
import it.unitn.disi.utils.collections.Triplet;
import it.unitn.disi.utils.logging.IProgressTracker;
import it.unitn.disi.utils.logging.Progress;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;

import org.lambda.functions.implementations.F2;

/**
 * {@link TEExperimentHelper} is a helper class which puts together the
 * machinery to run the WOSN paper experiments. In essence, it allows the
 * computation of:
 * 
 * <ol>
 * <li>the least-cost path estimate</li>
 * <li>the brute-force simulations estimate</li>
 * <li>the top-k brute-force simulations estimate</li>
 * </ol>
 * 
 * The "driver" for this class depends on the experiment we're running.
 * 
 * @author giuliano
 */
public class TEExperimentHelper {

	public static final int ALL_CORES = -1;

	// ------------------------------------------------------------------------
	// Helpers for configuration of the top-k estimator.
	// ------------------------------------------------------------------------

	public static final F2<IndexedNeighborGraph, double[][], ITopKEstimator> EDGE_DISJOINT = new F2<IndexedNeighborGraph, double[][], ITopKEstimator>(
			LightweightStaticGraph.fromAdjacency(new int[][] {}),
			new double[][] {}) {
		{
			ret(new DunnTopK(a, b, Mode.EdgeDisjoint));
		}
	};

	public static final F2<IndexedNeighborGraph, double[][], ITopKEstimator> VERTEX_DISJOINT = new F2<IndexedNeighborGraph, double[][], ITopKEstimator>(
			LightweightStaticGraph.fromAdjacency(new int[][] {}),
			new double[][] {}) {
		{
			ret(new DunnTopK(a, b, Mode.VertexDisjoint));
		}
	};

	public static final F2<IndexedNeighborGraph, double[][], ITopKEstimator> YENS = new F2<IndexedNeighborGraph, double[][], ITopKEstimator>(
			LightweightStaticGraph.fromAdjacency(new int[][] {}),
			new double[][] {}) {
		{
			ret(new LawlerTopK(a, b));
		}
	};

	public static F2<IndexedNeighborGraph, double[][], ITopKEstimator> estimator(
			String name) {
		if (name.equals("yen")) {
			return TEExperimentHelper.YENS;
		}

		else if (name.equals("vd")) {
			return TEExperimentHelper.VERTEX_DISJOINT;
		}

		else if (name.equals("ed")) {
			return TEExperimentHelper.EDGE_DISJOINT;
		}

		throw new IllegalArgumentException();
	}

	// ------------------------------------------------------------------------
	//
	// ------------------------------------------------------------------------

	private final YaoChurnConfigurator fYaoConf;

	private final int fRepetitions;

	private final double fBurnin;

	private final TaskExecutor fExecutor;

	/**
	 * Creates a new helper.
	 * 
	 * @param yaoConf
	 * @param estimator
	 * @param repetitions
	 * @param burnin
	 */
	public TEExperimentHelper(YaoChurnConfigurator yaoConf, int cores,
			int repetitions, double burnin) {
		fYaoConf = yaoConf;
		fBurnin = burnin;
		fRepetitions = repetitions;
		fExecutor = new TaskExecutor(cores, 10);
	}

	public void shutdown(boolean wait) throws InterruptedException {
		fExecutor.shutdown(wait);
	}

	/**
	 * Computes the propagation latency upper bound by summing the expected
	 * inter-activation times along the least-cost paths connecting a vertex to
	 * all others.
	 * 
	 * @param graph
	 *            the graph over which to compute the upper bound.
	 * @param source
	 *            the source from which to compute the upper bound.
	 * @param w
	 *            the weight matrix containing the expected inter-activation
	 *            times for each edge.
	 * 
	 * @return an array containing the estimated propagation delay from the
	 *         source to all other vertices in the graph.
	 */
	public double[] upperBound(IndexedNeighborGraph graph, int source,
			double[][] w) {
		System.err.println("Estimating for source " + source + ".");

		double[] minDists = new double[graph.size()];
		int[] previous = new int[graph.size()];
		Arrays.fill(previous, Integer.MAX_VALUE);
		GraphAlgorithms.dijkstra(graph, source, w, minDists, previous);

		return minDists;
	}

	/**
	 * Performs a "brute-force simulation" to estimate the propagation delay
	 * between a source and all other vertices in the graph. The addition of the
	 * sourceStart and sourceEnd parameters allow simulations to be multiplexed
	 * on top of one single churn simulation, saving on the burn-in time. With
	 * the latest version, a {@link CloudSim} is also stacked on top.
	 * 
	 * TODO separate {@link CloudSim} stacking and make this method more
	 * generic.
	 * 
	 * @param taskStr
	 *            a title for the progress tracker for this task.
	 * 
	 * @param graph
	 *            the graph over which to estimate delays.
	 * 
	 * @param sourceStart
	 *            source interval start.
	 * 
	 * @param sourceEnd
	 *            source interval end.
	 * 
	 * @param lIs
	 *            the li (session length) availability parameter for each
	 *            vertex.
	 * 
	 * @param dIs
	 *            the di (inter-session length) availability parameter for each
	 *            vertex.
	 * 
	 * @param ids
	 *            the original ids of the vertices (for printing activations).
	 * 
	 * @param sampleActivations
	 *            whether to sample edge activation or not (makes simulation
	 *            even slower).
	 * 
	 * @return a {@link SimulationResults} array, where each entry corresponds
	 *         to a source (the {@link Integer}, together with the corresponding
	 *         propagation delay estimates from that source to all other
	 *         vertices in the graph, as well as the {@link CloudSim} estimates.
	 * @throws Exception
	 */
	public List<? extends INodeMetric<?>>[] bruteForceSimulate(String taskStr,
			final IndexedNeighborGraph graph, final int sourceStart,
			final int sourceEnd, final double[] lIs, final double[] dIs,
			final int root, final int[] fixedNodes, final boolean cloudSim,
			final boolean monitorComponents, boolean adaptive) throws Exception {

		int sources = sourceEnd - sourceStart + 1;

		fExecutor.start(taskStr, fRepetitions);

		// Decoupled submission thread.
		Thread submitter = new Thread(new Runnable() {

			public void run() {
				for (int j = 0; j < fRepetitions && !Thread.interrupted(); j++) {
					SimulationTaskBuilder builder = new SimulationTaskBuilder(
							graph, root, lIs, dIs, fYaoConf);
					// Stacks sources.
					for (int i = sourceStart; i <= sourceEnd; i++) {
						if (fixedNodes == null) {
							builder.addConnectivitySimulation(i, "ed", "rd");
						} else {
							builder.addConnectivitySimulation(i, fixedNodes,
									null, "ed", "rd");
						}

						if (monitorComponents) {
							builder.andComponentTracker(i);
						}

						if (cloudSim) {
							builder.addCloudSimulation(i);
						}
					}

					try {
						fExecutor.submit(builder.simulationTask(fBurnin));
					} catch (InterruptedException e) {
						break;
					} catch (RejectedExecutionException ex) {
						System.err
								.println("Batch terminated, submission halted.");
						break;
					}
				}
			}
		}, "Task Submission Thread");
		submitter.start();

		@SuppressWarnings("unchecked")
		List<AvgAccumulation>[] metric = new List[sources];
		for (int i = 0; i < metric.length; i++) {
			metric[i] = new ArrayList<AvgAccumulation>();
		}

		for (int i = 0; i < fRepetitions; i++) {
			Object taskResult = fExecutor.consume();
			if (taskResult instanceof Throwable) {
				Throwable ex = (Throwable) taskResult;
				ex.printStackTrace();
				continue;
			}

			SimulationTask task = (SimulationTask) taskResult;
			int[] tSources = task.sources();

			if (((Integer) task.id()) != graph.size()) {
				throw new IllegalStateException(((Integer) task.id()) + " != "
						+ graph.size());
			}

			boolean done = true;
			for (int source : tSources) {
				@SuppressWarnings("unchecked")
				List<INodeMetric<Double>> result = (List<INodeMetric<Double>>) task
						.metric(source);
				for (INodeMetric<Double> networkMetric : result) {
					done &= addMatchingMetric(metric[source - sourceStart],
							networkMetric, graph.size());
				}
			}

			if (done && adaptive) {
				System.err.println("Simulation done after " + i
						+ " repetitions.");
				fExecutor.cancelBatch();
				submitter.join();
				break;
			}
		}

		return metric;
	}

	@SuppressWarnings("unchecked")
	public List<INodeMetric<Double>> bruteForceSimulateMulti(
			IndexedNeighborGraph graph, int root, int source, double[] li,
			double[] di, int[] idMap) throws Exception {

		IProgressTracker tracker = Progress.newTracker("root: " + root
				+ ", size: " + li.length, fRepetitions);

		SimulationTaskBuilder stb = new SimulationTaskBuilder(graph, root, li,
				di, fYaoConf, idMap);
		stb.addMultiConnectivitySimulation(source, fRepetitions,
				new AvgAccumulation("ed", graph.size()), new AvgAccumulation(
						"rd", graph.size()), tracker);
		SimulationTask task = stb.simulationTask(fBurnin);

		task.call();

		return (List<INodeMetric<Double>>) task.metric(source);
	}

	private boolean addMatchingMetric(List<AvgAccumulation> list,
			INodeMetric<Double> networkMetric, int length) {

		AvgAccumulation aggregate = null;

		for (AvgAccumulation candidate : list) {
			if (candidate.id().equals(networkMetric.id())) {
				aggregate = candidate;
			}
		}

		if (aggregate == null) {
			aggregate = new AvgAccumulation(networkMetric.id(), length,
					AvgEvaluator.DEFAULT_PRECISION, (30 / 3600.0));
			list.add(aggregate);
		}

		aggregate.add(networkMetric);

		return aggregate.isPreciseEnough();
	}

	/**
	 * Convenience method with a simpler signature for calling
	 * {@link #bruteForceSimulate(String, IndexedNeighborGraph, int, int, double[][], int[], boolean)}
	 * when there is a single source to be simulated.
	 * 
	 * @see #bruteForceSimulate(String, IndexedNeighborGraph, int, int,
	 *      double[][], int[], boolean)
	 */
	public List<? extends INodeMetric<?>> bruteForceSimulate(String taskStr,
			IndexedNeighborGraph graph, int root, int source, double[] lIs,
			double[] dIs, int[] cloudNodes, boolean cloudSim,
			boolean monitorComponents, boolean adaptive) throws Exception {
		return bruteForceSimulate(taskStr, graph, source, source, lIs, dIs,
				root, cloudNodes, cloudSim, monitorComponents, adaptive)[0];
	}

	/**
	 * Given a pair of vertices, produces the propagation delay estimate between
	 * them over the subgraph induced by the top-k, least-cost paths connecting
	 * them.
	 * 
	 * @param taskString
	 *            a task string to be shown in the progress indicator.
	 * @param graph
	 *            the graph to which the pair belongs.
	 * @param source
	 *            the source vertex.
	 * @param target
	 *            the target vertex.
	 * @param w
	 *            the weight matrix (with the inter-activation times for the
	 *            edges).
	 * @param lIs
	 *            the li (session length) availability parameter for each
	 *            vertex.
	 * @param dIs
	 *            the di (inter-session length) availability parameter for each
	 *            vertex.
	 * @param k
	 *            the k of the top-k.
	 * @param ids
	 *            the id mapping in the original graph.
	 * 
	 * @return a {@link Pair} containing the top-k estimate and the top-k
	 *         subgraph.
	 */
	public Triplet<IndexedNeighborGraph, PathEntry[], Double> topKEstimate(
			String taskString, IndexedNeighborGraph graph,
			F2<IndexedNeighborGraph, double[][], ITopKEstimator> estimator,
			int root, int source, int target, double[][] w, double[] lIs,
			double[] dIs, int k, int[] ids) throws Exception {

		ITopKEstimator tpk = estimator.call(graph, w);
		return topKEstimate(taskString, graph, root, source, target, lIs, dIs,
				k, ids, tpk);
	}

	public Triplet<IndexedNeighborGraph, PathEntry[], Double> topKEstimate(
			String taskString, IndexedNeighborGraph graph, int root,
			int source, int target, double[] lIs, double[] dIs, int k,
			int[] ids, ITopKEstimator tpk) throws Exception {

		// 1. computes the top-k shortest paths between u and w.
		ArrayList<? extends PathEntry> paths = tpk.topKShortest(source, target,
				k);

//		for (PathEntry entry : paths) {
//			StringBuffer buffer = new StringBuffer("TPK:[ ");
//			for (int i = 0; i < entry.path.length; i++) {
//				buffer.append("(");
//				buffer.append(entry.path[i]);
//				buffer.append(", ");
//				buffer.append(lIs[entry.path[i]]);
//				buffer.append(", ");
//				buffer.append(dIs[entry.path[i]]);
//				buffer.append("), ");
//			}
//			buffer.append("]");
//			System.out.println(buffer);
//		}

		for (int i = 0; i < paths.size(); i++) {
			System.err.println(Arrays.toString(paths.get(i).path));
		}

		int[] vertexes = vertexesOf(paths);
		LightweightStaticGraph kPathGraph = LightweightStaticGraph.subgraph(
				(LightweightStaticGraph) graph, vertexes);

		System.err.println("source " + ids[source] + ", target " + ids[target]
				+ ", size " + vertexes.length);

		// 2. runs a connectivity simulation on the subgraph
		// composed by the top-k shortest paths.
		double liSub[] = new double[vertexes.length];
		double diSub[] = new double[vertexes.length];
		for (int j = 0; j < vertexes.length; j++) {
			liSub[j] = lIs[vertexes[j]];
			diSub[j] = dIs[vertexes[j]];
		}

		int remappedSource = indexOf(source, vertexes);
		int remappedTarget = indexOf(target, vertexes);

		INodeMetric<Double> estimate = Utils.lookup(
				bruteForceSimulateMulti(kPathGraph, root, remappedSource,
						liSub, diSub, null), "ed", Double.class);

		return new Triplet<IndexedNeighborGraph, PathEntry[], Double>(
				kPathGraph, paths.toArray(new PathEntry[paths.size()]),
				estimate.getMetric(remappedTarget));
	}

	private int indexOf(int element, int[] vertexes) {
		for (int i = 0; i < vertexes.length; i++) {
			if (element == vertexes[i]) {
				return i;
			}
		}

		throw new NoSuchElementException();
	}

	private int[] vertexesOf(ArrayList<? extends PathEntry> topKShortest) {
		Set<Integer> vertexSet = new HashSet<Integer>();
		for (PathEntry entry : topKShortest) {
			for (int i = 0; i < entry.path.length; i++) {
				vertexSet.add(entry.path[i]);
			}
		}

		int[] vertexes = new int[vertexSet.size()];
		int i = 0;
		for (Integer element : vertexSet) {
			vertexes[i++] = element;
		}

		Arrays.sort(vertexes);
		return vertexes;
	}

}
