package it.unitn.disi.churn.connectivity;

import it.unitn.disi.churn.AssignmentReader;
import it.unitn.disi.churn.GraphConfigurator;
import it.unitn.disi.churn.IValueObserver;
import it.unitn.disi.churn.IncrementalStatsAdapter;
import it.unitn.disi.churn.YaoChurnConfigurator;
import it.unitn.disi.churn.intersync.ParallelParwiseSyncEstimator;
import it.unitn.disi.churn.intersync.ParallelParwiseSyncEstimator.EdgeTask;
import it.unitn.disi.churn.intersync.ParallelParwiseSyncEstimator.GraphTask;
import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.large.catalog.IGraphProvider;
import it.unitn.disi.statistics.StatUtils;
import it.unitn.disi.utils.CallbackThreadPoolExecutor;
import it.unitn.disi.utils.streams.PrefixedWriter;
import it.unitn.disi.utils.tabular.TableWriter;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.lambda.functions.implementations.F0;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.IResolver;
import peersim.config.ObjectCreator;
import peersim.util.IncrementalStats;

@AutoConfig
public class PairwiseEstimate implements ITransformer {

	private final TableWriter fSampleWriter = new TableWriter(new PrintWriter(
			new PrefixedWriter("SM:", new OutputStreamWriter(System.out))),
			"id", "source", "target", "ttcl", "ttc", "ttcu");

	@Attribute("repeats")
	private int fRepeats;
	
	@Attribute("cloud")
	private boolean fCloud;

	private GraphConfigurator fGraphConf;

	private YaoChurnConfigurator fYaoConf;

	public PairwiseEstimate(@Attribute(Attribute.AUTO) IResolver resolver) {
		fYaoConf = ObjectCreator.createInstance(YaoChurnConfigurator.class, "",
				resolver);
		fGraphConf = ObjectCreator.createInstance(GraphConfigurator.class, "",
				resolver);
	}

	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {
		IGraphProvider loader = fGraphConf.graphProvider();

		ParallelParwiseSyncEstimator ppse = new ParallelParwiseSyncEstimator(
				fRepeats);

		ExecutorService es = new CallbackThreadPoolExecutor<Object>(Runtime
				.getRuntime().availableProcessors(), ppse);

		AssignmentReader reader = new AssignmentReader(is, "id");
		while (reader.hasNext()) {
			int idx = Integer.parseInt(reader.currentRoot());
			int[] ids = loader.verticesOf(idx);
			double[][] lidi = reader.read(ids);
			IndexedNeighborGraph subgraph = loader.subgraph(idx);

			GraphTask gt = ppse.estimate(new F0<IValueObserver>() {
				{
					ret(new IncrementalStatsAdapter(new IncrementalStats()));
				};
			}, es, subgraph, lidi[AssignmentReader.LI],
					lidi[AssignmentReader.DI],
					fYaoConf.distributionGenerator(), fCloud, idx);

			gt.await();

			for (EdgeTask task : gt.edgeTasks) {

				IncrementalStats stats = ((IncrementalStatsAdapter) task.stats)
						.getStats();

				fSampleWriter.set("id", idx);
				fSampleWriter.set("source", ids[task.i]);
				fSampleWriter.set("target", ids[task.j]);
				fSampleWriter
						.set("ttcl", StatUtils.lowerConfidenceLimit(stats));
				fSampleWriter.set("ttc", stats.getAverage());
				fSampleWriter
						.set("ttcu", StatUtils.upperConfidenceLimit(stats));
				fSampleWriter.emmitRow();
			}
		}

		es.shutdown();
		es.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
	}

}
