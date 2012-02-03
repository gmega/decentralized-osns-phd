package it.unitn.disi.churn.connectivity;

import it.unitn.disi.churn.IValueObserver;
import it.unitn.disi.churn.IncrementalStatsAdapter;
import it.unitn.disi.churn.intersync.ParallelParwiseSyncEstimator;
import it.unitn.disi.churn.intersync.ParallelParwiseSyncEstimator.EdgeTask;
import it.unitn.disi.churn.intersync.ParallelParwiseSyncEstimator.GraphTask;
import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.large.catalog.IGraphProvider;
import it.unitn.disi.statistics.StatUtils;
import it.unitn.disi.utils.CallbackThreadPoolExecutor;
import it.unitn.disi.utils.OrderingUtils;
import it.unitn.disi.utils.streams.PrefixedWriter;
import it.unitn.disi.utils.tabular.TableWriter;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.lambda.functions.implementations.F0;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.util.IncrementalStats;

@AutoConfig
public class PrepareSample extends YaoGraphExperiment implements ITransformer {

	private final TableWriter fSampleWriter = new TableWriter(new PrintWriter(
			new PrefixedWriter("SM:", new OutputStreamWriter(System.out))),
			"id", "source", "target", "ttcl", "ttc", "ttcu");

	private final TableWriter fAssignmentWriter = new TableWriter(
			new PrintWriter(new PrefixedWriter("A:", new OutputStreamWriter(
					System.out))), "id", "node", "li", "di");

	@Attribute("samples")
	private String fSamples;

	@Attribute("repeats")
	private int fRepeats;

	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {
		IGraphProvider loader = graphProvider();
		Random random = new Random();

		int[] samples = sampleList(loader, random);

		ParallelParwiseSyncEstimator ppse = new ParallelParwiseSyncEstimator(
				fMode, fAssignments, fRepeats);

		ExecutorService es = new CallbackThreadPoolExecutor<Object>(Runtime
				.getRuntime().availableProcessors(), ppse);

		for (int i = 0; i < samples.length; i++) {
			int idx = samples[i];

			IndexedNeighborGraph subgraph = loader.subgraph(idx);
			int[] ids = loader.verticesOf(idx);

			GraphTask gt = ppse.estimate(new F0<IValueObserver>() {
				{
					ret(new IncrementalStatsAdapter(new IncrementalStats()));
				};
			}, es, subgraph, idx);

			for (int j = 0; j < subgraph.size(); j++) {
				fAssignmentWriter.set("id", idx);
				fAssignmentWriter.set("node", ids[j]);
				fAssignmentWriter.set("li", gt.lIs[j]);
				fAssignmentWriter.set("di", gt.dIs[j]);
				fAssignmentWriter.emmitRow();
			}

			gt.await();

			for (EdgeTask task : gt.edgeTasks) {
				
				IncrementalStats stats = (IncrementalStats) task.stats;
				
				fSampleWriter.set("id", idx);
				fSampleWriter.set("source", ids[task.i]);
				fSampleWriter.set("target", ids[task.j]);
				fSampleWriter.set("ttcl",
						StatUtils.lowerConfidenceLimit(stats));
				fSampleWriter.set("ttc", stats.getAverage());
				fSampleWriter.set("ttcu",
						StatUtils.upperConfidenceLimit(stats));
				fSampleWriter.emmitRow();
			}
		}

		es.shutdown();
		es.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
	}

	private int[] sampleList(IGraphProvider loader, Random random) {
		int[] samples;
		if (fSamples.contains(",")) {
			String[] sampleRoots = fSamples.split(",");
			samples = new int[sampleRoots.length];
			for (int i = 0; i < sampleRoots.length; i++) {
				samples[i] = Integer.parseInt(sampleRoots[i]);
			}
		} else {
			int[] permute = new int[loader.size()];
			for (int i = 0; i < loader.size(); i++) {
				permute[i] = i;
			}
			OrderingUtils.permute(0, permute.length, permute, random);
			samples = Arrays.copyOf(permute, Integer.parseInt(fSamples));
		}
		return samples;
	}

}
