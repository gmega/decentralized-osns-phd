package it.unitn.disi.churn.connectivity;

import it.unitn.disi.churn.connectivity.p2p.Utils;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.analysis.DunnTopK;
import it.unitn.disi.graph.analysis.DunnTopK.Mode;
import it.unitn.disi.graph.analysis.GraphAlgorithms;
import it.unitn.disi.graph.analysis.ITopKEstimator;
import it.unitn.disi.graph.analysis.LawlerTopK;
import it.unitn.disi.graph.analysis.PathEntry;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;
import it.unitn.disi.simulator.concurrent.TaskExecutor;
import it.unitn.disi.simulator.measure.INodeMetric;
import it.unitn.disi.simulator.measure.SumAccumulation;
import it.unitn.disi.simulator.yao.YaoChurnConfigurator;
import it.unitn.disi.utils.collections.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

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
		fExecutor = new TaskExecutor(cores);
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
	 * @param graph
	 *            the graph over which to estimate the propagation delay.
	 * @param sourceStart
	 *            source interval start.
	 * @param sourceEnd
	 *            source interval end.
	 * @param lIs
	 *            the li (session length) availability parameter for each
	 *            vertex.
	 * @param dIs
	 *            the di (inter-session length) availability parameter for each
	 *            vertex.
	 * @param ids
	 *            the original ids of the vertices (for printing activations).
	 * @param sampleActivations
	 *            whether to sample edge activation or not (makes simulation
	 *            even slower).
	 * @return a {@link SimulationResults} array, where each entry corresponds
	 *         to a source (the {@link Integer}, together with the corresponding
	 *         propagation delay estimates from that source to all other
	 *         vertices in the graph, as well as the {@link CloudSim} estimates.
	 * @throws Exception
	 */
	public List<? extends INodeMetric<?>>[] bruteForceSimulate(String taskStr,
			IndexedNeighborGraph graph, int sourceStart, int sourceEnd,
			double[] lIs, double[] dIs, int root, int[] ids, int[] cloudNodes,
			boolean sampleActivations, boolean cloudSim,
			boolean monitorComponents) throws Exception {

		ActivationSampler sampler = sampleActivations ? new ActivationSampler(
				graph) : null;

		int sources = sourceEnd - sourceStart + 1;

		fExecutor.start(taskStr, fRepetitions);

		for (int j = 0; j < fRepetitions; j++) {
			SimulationTaskBuilder builder = new SimulationTaskBuilder(graph,
					ids, root);
			// Stacks sources.
			for (int i = sourceStart; i <= sourceEnd; i++) {
				builder.addConnectivitySimulation(i, cloudNodes, null);
				if (monitorComponents) {
					builder.andComponentTracker();
				}

				if (cloudSim) {
					builder.addCloudSimulation(i);
				}
			}

			fExecutor.submit(builder
					.simulationTask(lIs, dIs, fBurnin, fYaoConf));
		}

		@SuppressWarnings("unchecked")
		List<SumAccumulation>[] metric = new List[sources];
		for (int i = 0; i < metric.length; i++) {
			metric[i] = new ArrayList<SumAccumulation>();
		}

		for (int i = 0; i < fRepetitions; i++) {
			Object taskResult = fExecutor.consume();
			if (taskResult instanceof Throwable) {
				Throwable ex = (Throwable) taskResult;
				ex.printStackTrace();
				continue;
			}

			@SuppressWarnings("unchecked")
			Pair<Integer, List<INodeMetric<Double>>>[] results = (Pair<Integer, List<INodeMetric<Double>>>[]) taskResult;

			for (Pair<Integer, List<INodeMetric<Double>>> result : results) {
				for (INodeMetric<Double> networkMetric : result.b) {
					addMatchingMetric(metric[result.a - sourceStart],
							networkMetric, graph.size());
				}
			}
		}

		// Print activations.
		if (sampler != null) {
			sampler.printActivations(ids);
		}

		return metric;
	}

	private void addMatchingMetric(List<SumAccumulation> list,
			INodeMetric<Double> networkMetric, int length) {
		for (SumAccumulation aggregate : list) {
			if (aggregate.id().equals(networkMetric.id())) {
				aggregate.add(networkMetric);
			}
		}
		list.add(new SumAccumulation(networkMetric, length));
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
			double[] dIs, int[] ids, int[] cloudNodes,
			boolean sampleActivations, boolean cloudSim,
			boolean monitorComponents) throws Exception {
		return bruteForceSimulate(taskStr, graph, source, source, lIs, dIs,
				root, ids, cloudNodes, sampleActivations, cloudSim,
				monitorComponents)[0];
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
	public Pair<IndexedNeighborGraph, Double> topKEstimate(String taskString,
			IndexedNeighborGraph graph,
			F2<IndexedNeighborGraph, double[][], ITopKEstimator> estimator,
			int source, int target, double[][] w, double[] lIs, double[] dIs,
			int k, int[] ids) throws Exception {

		ITopKEstimator tpk = estimator.call(graph, w);

		System.out.println("Source: " + ids[source] + " Target: " + ids[target]
				+ ".");

		// 1. computes the top-k shortest paths between u and w.
		int[] vertexes = vertexesOf(tpk.topKShortest(source, target, k));
		LightweightStaticGraph kPathGraph = LightweightStaticGraph.subgraph(
				(LightweightStaticGraph) graph, vertexes);

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
				bruteForceSimulate(taskString, kPathGraph, remappedSource,
						remappedSource, liSub, diSub, ids, null, false, false,
						false), "ed", Double.class);

		return new Pair<IndexedNeighborGraph, Double>(kPathGraph,
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
