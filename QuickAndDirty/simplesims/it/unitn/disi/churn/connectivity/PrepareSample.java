package it.unitn.disi.churn.connectivity;

import it.unitn.disi.churn.BaseChurnSim;
import it.unitn.disi.churn.IChurnSim;
import it.unitn.disi.churn.RenewalProcess;
import it.unitn.disi.churn.RenewalProcess.State;
import it.unitn.disi.churn.intersync.SyncExperiment;
import it.unitn.disi.churn.intersync.TrueSyncEstimator;
import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.large.catalog.IGraphProvider;
import it.unitn.disi.network.churn.yao.YaoInit.IAverageGenerator;
import it.unitn.disi.network.churn.yao.YaoInit.IDistributionGenerator;
import it.unitn.disi.statistics.StatUtils;
import it.unitn.disi.utils.CallbackThreadPoolExecutor;
import it.unitn.disi.utils.IExecutorCallback;
import it.unitn.disi.utils.OrderingUtils;
import it.unitn.disi.utils.logging.Progress;
import it.unitn.disi.utils.logging.ProgressTracker;
import it.unitn.disi.utils.streams.PrefixedWriter;
import it.unitn.disi.utils.tabular.TableWriter;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.util.IncrementalStats;

@AutoConfig
public class PrepareSample extends YaoGraphExperiment implements ITransformer, IExecutorCallback<Object> {

	private final TableWriter fSampleWriter = new TableWriter(new PrintWriter(
			new PrefixedWriter("SM:", new OutputStreamWriter(System.out))),
			"id", "source", "target", "ttcl", "ttc", "ttcu");

	private final TableWriter fAssignmentWriter = new TableWriter(
			new PrintWriter(new PrefixedWriter("A:", new OutputStreamWriter(
					System.out))), "id", "node", "li", "di");

	@Attribute("samples")
	private String fSamples;

	@Attribute("burnin")
	private double fBurnin;

	@Attribute("repeats")
	private int fRepeats;

	private ProgressTracker fTracker;

	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {
		IGraphProvider loader = graphProvider();

		Random random = new Random();

		IDistributionGenerator distGen = distributionGenerator();
		IAverageGenerator avgGen = averageGenerator();

		int[] samples = sampleList(loader, random);

		for (int i = 0; i < samples.length; i++) {
			int idx = samples[i];

			IndexedNeighborGraph subgraph = loader.subgraph(idx);
			int[] ids = loader.verticesOf(idx);

			double[] lIs = new double[ids.length];
			double[] dIs = new double[ids.length];

			for (int j = 0; j < ids.length; j++) {
				lIs[j] = avgGen.nextLI();
				dIs[j] = avgGen.nextDI();

				fAssignmentWriter.set("id", idx);
				fAssignmentWriter.set("node", ids[j]);
				fAssignmentWriter.set("li", lIs[j]);
				fAssignmentWriter.set("di", dIs[j]);
				fAssignmentWriter.emmitRow();
			}

			ArrayList<TTCTask> tasks = new ArrayList<TTCTask>();

			// Now estimates the TTC for all edges, in parallel.
			for (int j = 0; j < lIs.length; j++) {
				for (int k = 0; k < dIs.length; k++) {
					if (j == k) {
						continue;
					}
					if (subgraph.isEdge(j, k)) {
						tasks.add(ttcTask(j, k, lIs, dIs, distGen));
					}
				}
			}

			ExecutorService es = new CallbackThreadPoolExecutor<Object>(Runtime
					.getRuntime().availableProcessors(), this);

			initializeProgress(i, tasks.size());

			for (TTCTask task : tasks) {
				synchronized (task) {
					es.submit(task.sim);
				}
			}

			es.shutdown();
			es.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);

			for (TTCTask task : tasks) {
				synchronized (task) {
					fSampleWriter.set("id", idx);
					fSampleWriter.set("source", ids[task.i]);
					fSampleWriter.set("target", ids[task.j]);
					fSampleWriter.set("ttcl",
							StatUtils.lowerConfidenceLimit(task.stats));
					fSampleWriter.set("ttc", task.stats.getAverage());
					fSampleWriter.set("ttcu",
							StatUtils.upperConfidenceLimit(task.stats));
					fSampleWriter.emmitRow();
				}
			}
		}
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

	private TTCTask ttcTask(int i, int j, double[] lIs, double[] dIs,
			IDistributionGenerator distGen) {

		RenewalProcess pI = new RenewalProcess(i,
				distGen.uptimeDistribution(lIs[i]),
				distGen.downtimeDistribution(dIs[i]), State.down);
		RenewalProcess pJ = new RenewalProcess(j,
				distGen.uptimeDistribution(lIs[j]),
				distGen.downtimeDistribution(dIs[j]), State.down);

		ArrayList<IChurnSim> sims = new ArrayList<IChurnSim>();
		TrueSyncEstimator sexp = new TrueSyncEstimator(fRepeats);
		sims.add(sexp);

		ArrayList<Object> cookies = new ArrayList<Object>();
		IncrementalStats stats = new IncrementalStats();
		cookies.add(stats);

		BaseChurnSim churnSim = new BaseChurnSim(
				new RenewalProcess[] { pI, pJ }, sims, cookies, 0.0);

		return new TTCTask(churnSim, sexp, stats, i, j);
	}

	static class TTCTask {
		final BaseChurnSim sim;
		final IChurnSim sexp;
		final IncrementalStats stats;
		final int i;
		final int j;

		public TTCTask(BaseChurnSim sim, IChurnSim sexp,
				IncrementalStats stats, int i, int j) {
			this.sim = sim;
			this.sexp = sexp;
			this.i = i;
			this.j = j;
			this.stats = stats;
		}
	}

	private synchronized void initializeProgress(int sampleId, int size) {
		fTracker = Progress
				.newTracker("est. TTC sample " + sampleId + "", size);
		fTracker.startTask();
	}

	@Override
	public synchronized void taskFailed(Future<Object> task, Exception ex) {
		ex.printStackTrace();
		fTracker.tick();
	}

	@Override
	public synchronized void taskDone(Object result) {
		fTracker.tick();
	}
}
