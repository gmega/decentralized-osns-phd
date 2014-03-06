package it.unitn.disi.churn.connectivity;

import it.unitn.disi.churn.config.AssignmentReader;
import it.unitn.disi.churn.config.AssignmentReader.Assignment;
import it.unitn.disi.churn.intersync.ParallelParwiseEstimator;
import it.unitn.disi.churn.intersync.ParallelParwiseEstimator.EdgeTask;
import it.unitn.disi.churn.intersync.ParallelParwiseEstimator.GraphTask;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.codecs.AdjListGraphDecoder;
import it.unitn.disi.graph.codecs.GraphCodecHelper;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;
import it.unitn.disi.simulator.churnmodel.yao.YaoChurnConfigurator;
import it.unitn.disi.simulator.measure.INodeMetric;
import it.unitn.disi.simulator.measure.IncrementalStatsAdapter;
import it.unitn.disi.utils.CallbackThreadPoolExecutor;
import it.unitn.disi.utils.streams.ResettableFileInputStream;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import org.lambda.functions.implementations.F0;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.util.IncrementalStats;

/**
 * Generates egonetwork/source samples. This implementation is only suitable for
 * <it>sparse</it> samples. I wouldn't go over 10% of the network with it. 
 * 
 * @author giuliano
 */
@AutoConfig
public class SimpleSimulation implements Runnable {

	@Attribute("graph")
	private String fGraph;

	@Attribute("cores")
	int fCores;

	@Attribute("reps")
	int fReps;

	@Attribute("assignments")
	private String fAssignments;

	private YaoChurnConfigurator fYaoChurn = new YaoChurnConfigurator("TE",
			"yao");

	@Override
	public void run() {
		try {
			run0();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	private void run0() throws Exception {

		IndexedNeighborGraph graph = LightweightStaticGraph
				.load(GraphCodecHelper.createDecoder(AdjListGraphDecoder.class,
						new ResettableFileInputStream(new File(fGraph))));

		int ids[] = new int[graph.size()];
		for (int i = 0; i < ids.length; i++) {
			ids[i] = i;
		}

		AssignmentReader reader = new AssignmentReader(new FileInputStream(
				new File(fAssignments)), "id");

		Assignment assignment = reader.read(ids);

		simulate(graph, ids, assignment);
		pairwiseEstimate(graph, ids, assignment);

	}

	private void pairwiseEstimate(IndexedNeighborGraph graph, int[] ids,
			Assignment e) throws InterruptedException, ExecutionException {

		ParallelParwiseEstimator ppe = new ParallelParwiseEstimator();
		ExecutorService executor = new CallbackThreadPoolExecutor<Object>(
				fCores, ppe);

		GraphTask gt = ppe.estimate(
				new F0<it.unitn.disi.simulator.measure.IValueObserver>() {
					{
						ret(new IncrementalStatsAdapter(new IncrementalStats()));
					};
				}, executor, graph, fReps, e.li, e.di,
				fYaoChurn.distributionGenerator(), false, 0, ids);

		gt.await();

		for (EdgeTask task : gt.edgeTasks) {
			IncrementalStatsAdapter adapt = (IncrementalStatsAdapter) task.stats;
			IncrementalStats stats = adapt.getStats();
			System.out.println("PE:" + task.i + " " + task.j + " "
					+ stats.getAverage());
		}

	}

	private void simulate(IndexedNeighborGraph graph, int[] ids,
			Assignment assignment) throws Exception {
		TEExperimentHelper helper = new TEExperimentHelper(fYaoChurn, fCores,
				fReps, 48.0);

		List<? extends INodeMetric<?>> metrics = helper
				.bruteForceSimulateMulti(graph, 0, 0, assignment.li,
						assignment.di, null);
		/*
		 * bruteForceSimulate( "simulate", graph, 0, 0, assignment.li,
		 * assignment.di, ids, new int[] {}, false, false, false);
		 */

		INodeMetric<Double> metric = findMetric(metrics, "ed");

		System.out.println("node ed");
		for (int i = 0; i < ids.length; i++) {
			System.out.println("SR:" + i + " " + (metric.getMetric(i) / fReps));
		}
	}

	private INodeMetric<Double> findMetric(
			List<? extends INodeMetric<?>> metrics, String string) {
		for (INodeMetric<?> metric : metrics) {
			if (metric.id().equals(string)) {
				return (INodeMetric<Double>) metric;
			}
		}

		throw new NoSuchElementException();
	}

}
