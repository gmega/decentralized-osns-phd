package it.unitn.disi.churn.connectivity.p2p;

import it.unitn.disi.churn.IValueObserver;
import it.unitn.disi.churn.IncrementalStatsAdapter;
import it.unitn.disi.churn.intersync.ParallelParwiseEstimator;
import it.unitn.disi.churn.intersync.ParallelParwiseEstimator.EdgeTask;
import it.unitn.disi.churn.intersync.ParallelParwiseEstimator.GraphTask;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.newscasting.experiments.schedulers.IScheduleIterator;
import it.unitn.disi.statistics.StatUtils;
import it.unitn.disi.utils.CallbackThreadPoolExecutor;
import it.unitn.disi.utils.streams.PrefixedWriter;
import it.unitn.disi.utils.tabular.TableWriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import org.lambda.functions.implementations.F0;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.IResolver;
import peersim.util.IncrementalStats;

@AutoConfig
public class PairwiseEstimateWorker extends AbstractWorker {

	public PairwiseEstimateWorker(@Attribute(Attribute.AUTO) IResolver resolver)
			throws IOException {
		super(resolver, "id", "id");
	}

	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {
		TableWriter writer = new TableWriter(new PrefixedWriter("ES:", oup),
				"id", "source", "target", "ttc", "ttcl", "ttcu");

		ParallelParwiseEstimator ppe = new ParallelParwiseEstimator();
		CallbackThreadPoolExecutor<Object> executor = null;

		try {
			executor = new CallbackThreadPoolExecutor<Object>(fCores, ppe);

			IScheduleIterator iterator = iterator();
			Integer row;
			while ((row = (Integer) iterator.nextIfAvailable()) != IScheduleIterator.DONE) {
				Experiment e = readExperiment(row);
				IndexedNeighborGraph graph = provider().subgraph(e.root);

				GraphTask gt = ppe.estimate(
						new F0<IValueObserver>() {
							{
								ret(new IncrementalStatsAdapter(
										new IncrementalStats()));
							};
						}, executor, graph, fRepeat, e.lis, e.dis, fYaoConfig
								.distributionGenerator(), false, e.root, e.ids);

				gt.await();

				for (EdgeTask task : gt.edgeTasks) {
					IncrementalStatsAdapter adapt = (IncrementalStatsAdapter) task.stats;
					IncrementalStats stats = adapt.getStats();

					writer.set("id", e.root);
					writer.set("source", e.ids[task.i]);
					writer.set("target", e.ids[task.j]);
					writer.set("ttcl", StatUtils.lowerConfidenceLimit(stats));
					writer.set("ttc", stats.getAverage());
					writer.set("ttcu", StatUtils.upperConfidenceLimit(stats));
					writer.emmitRow();
				}
			}

		} finally {
			if (executor != null) {
				executor.shutdown();
				executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
			}
		}
	}
}
