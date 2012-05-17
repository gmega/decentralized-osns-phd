package it.unitn.disi.churn.connectivity;

import it.unitn.disi.churn.config.AssignmentReader;
import it.unitn.disi.churn.config.AssignmentReader.Assignment;
import it.unitn.disi.churn.config.GraphConfigurator;
import it.unitn.disi.churn.config.YaoChurnConfigurator;
import it.unitn.disi.churn.intersync.ParallelParwiseEstimator;
import it.unitn.disi.churn.intersync.ParallelParwiseEstimator.EdgeTask;
import it.unitn.disi.churn.intersync.ParallelParwiseEstimator.GraphTask;
import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.large.catalog.IGraphProvider;
import it.unitn.disi.simulator.IValueObserver;
import it.unitn.disi.simulator.IncrementalStatsAdapter;
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

	private final TableWriter fSampleWriter;

	@Attribute("start")
	private int fStart;

	@Attribute("end")
	private int fEnd;

	@Attribute("repeats")
	private int fRepeats;

	@Attribute("cloud")
	private boolean fCloud;

	private GraphConfigurator fGraphConf;

	private YaoChurnConfigurator fYaoConf;

	private String[] fMeasureKeys;

	private static final int LOWER_CONFIDENCE = 0;
	private static final int AVERAGE = 1;
	private static final int UPPER_CONFIDENCE = 2;

	public PairwiseEstimate(@Attribute(Attribute.AUTO) IResolver resolver,
			@Attribute("measure") String measure) {
		fYaoConf = ObjectCreator.createInstance(YaoChurnConfigurator.class, "",
				resolver);
		fGraphConf = ObjectCreator.createInstance(GraphConfigurator.class, "",
				resolver);

		fMeasureKeys = new String[] { measure + "l", measure, measure + "u" };

		fSampleWriter = new TableWriter(new PrintWriter(new PrefixedWriter(
				"SM:", new OutputStreamWriter(System.out))), "id", "source",
				"target", fMeasureKeys[LOWER_CONFIDENCE],
				fMeasureKeys[AVERAGE], fMeasureKeys[UPPER_CONFIDENCE]);
	}

	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {
		IGraphProvider loader = fGraphConf.graphProvider();

		ParallelParwiseEstimator ppse = new ParallelParwiseEstimator();

		ExecutorService es = new CallbackThreadPoolExecutor<Object>(Runtime
				.getRuntime().availableProcessors(), ppse);

		int count = -1;
		System.out.println("A: id node li di");
		AssignmentReader reader = new AssignmentReader(is, "id");
		while (reader.hasNext()) {
			count++;
			if (count < fStart) {
				System.err.println("-- Skip neighborhood "
						+ reader.currentRoot() + ".");
				reader.skipCurrent();
				continue;
			}

			if (count >= fEnd) {
				break;
			}

			int idx = Integer.parseInt(reader.currentRoot());
			int[] ids = loader.verticesOf(idx);
			Assignment lidi = (Assignment) reader.read(ids);

			IndexedNeighborGraph subgraph = loader.subgraph(idx);

			if (idx == 257577) {
				for (int i = 1; i < fRepeats; i += 500) {

					GraphTask gt = ppse.estimate(new F0<IValueObserver>() {
						{
							ret(new IncrementalStatsAdapter(
									new IncrementalStats()));
						};
					}, es, subgraph, i, lidi.li, lidi.di, fYaoConf
							.distributionGenerator(), fCloud, idx, ids);

					gt.await();

					for (EdgeTask task : gt.edgeTasks) {

						IncrementalStats stats = ((IncrementalStatsAdapter) task.stats)
								.getStats();

						fSampleWriter.set("id", idx);
						fSampleWriter.set("source", ids[task.i]);
						fSampleWriter.set("target", ids[task.j]);
						fSampleWriter.set(fMeasureKeys[LOWER_CONFIDENCE],
								StatUtils.lowerConfidenceLimit(stats));
						fSampleWriter.set(fMeasureKeys[AVERAGE],
								stats.getAverage());
						fSampleWriter.set(fMeasureKeys[UPPER_CONFIDENCE],
								StatUtils.upperConfidenceLimit(stats));
						fSampleWriter.emmitRow();
					}
				}
			}
		}

		es.shutdown();
		es.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
	}

}
