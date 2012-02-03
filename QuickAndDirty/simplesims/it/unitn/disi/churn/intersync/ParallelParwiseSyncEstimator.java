package it.unitn.disi.churn.intersync;

import it.unitn.disi.churn.BaseChurnSim;
import it.unitn.disi.churn.IChurnSim;
import it.unitn.disi.churn.IncrementalStatsAdapter;
import it.unitn.disi.churn.RenewalProcess;
import it.unitn.disi.churn.RenewalProcess.State;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.network.churn.yao.YaoPresets;
import it.unitn.disi.network.churn.yao.YaoInit.IAverageGenerator;
import it.unitn.disi.network.churn.yao.YaoInit.IDistributionGenerator;
import it.unitn.disi.utils.IExecutorCallback;
import it.unitn.disi.utils.logging.Progress;
import it.unitn.disi.utils.logging.ProgressTracker;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import peersim.util.IncrementalStats;

public class ParallelParwiseSyncEstimator implements IExecutorCallback<Object> {

	private final String fMode;

	private final String fAvg;

	private final int fRepetitions;

	private ProgressTracker fTracker;

	// ------------------------------------------------------------------------

	public ParallelParwiseSyncEstimator(String mode, String avg, int repetitions) {
		fMode = mode;
		fAvg = avg;
		fRepetitions = repetitions;
	}

	// ------------------------------------------------------------------------

	public GraphTask estimate(ExecutorService es, int id, IndexedNeighborGraph g) {

		double[] lIs = new double[g.size()];
		double[] dIs = new double[g.size()];

		IAverageGenerator generator = YaoPresets.averageGenerator(fAvg);

		for (int j = 0; j < g.size(); j++) {
			lIs[j] = generator.nextLI();
			dIs[j] = generator.nextDI();
		}

		ArrayList<EdgeTask> tasks = new ArrayList<EdgeTask>();

		// Now estimates the TTC for all edges, in parallel.
		for (int j = 0; j < lIs.length; j++) {
			for (int k = 0; k < dIs.length; k++) {
				if (j == k) {
					continue;
				}
				if (g.isEdge(j, k)) {
					tasks.add(ttcTask(j, k, lIs, dIs,
							YaoPresets.mode(fMode, new Random())));
				}
			}
		}

		initializeProgress(id, tasks.size());

		for (EdgeTask task : tasks) {
			synchronized (task) {
				task.setFuture(es.submit(task.sim));
			}
		}

		return new GraphTask(lIs, dIs, tasks);
	}

	// ------------------------------------------------------------------------

	private EdgeTask ttcTask(int i, int j, double[] lIs, double[] dIs,
			IDistributionGenerator distGen) {

		RenewalProcess pI = new RenewalProcess(i,
				distGen.uptimeDistribution(lIs[i]),
				distGen.downtimeDistribution(dIs[i]), State.down);

		RenewalProcess pJ = new RenewalProcess(j,
				distGen.uptimeDistribution(lIs[j]),
				distGen.downtimeDistribution(dIs[j]), State.down);

		IncrementalStats stats = new IncrementalStats();

		ArrayList<IChurnSim> sims = new ArrayList<IChurnSim>();
		TrueSyncEstimator sexp = new TrueSyncEstimator(fRepetitions,
				new IncrementalStatsAdapter(stats));
		sims.add(sexp);

		BaseChurnSim churnSim = new BaseChurnSim(
				new RenewalProcess[] { pI, pJ }, sims, 0.0);

		return new EdgeTask(churnSim, stats, i, j);
	}

	// ------------------------------------------------------------------------

	private synchronized void initializeProgress(int sampleId, int size) {
		fTracker = Progress.newTracker("est. TTC sample " + sampleId + " ("
				+ size + ")", size);
		fTracker.startTask();
	}

	// ------------------------------------------------------------------------

	@Override
	public synchronized void taskFailed(Future<Object> task, Exception ex) {
		ex.printStackTrace();
		fTracker.tick();
	}

	@Override
	public synchronized void taskDone(Object result) {
		fTracker.tick();
	}

	// ------------------------------------------------------------------------

	public static class GraphTask {

		public final ArrayList<EdgeTask> edgeTasks;
		public final double[] lIs;
		public final double[] dIs;

		public GraphTask(double[] lIs, double dIs[], ArrayList<EdgeTask> tasks) {
			this.edgeTasks = tasks;
			this.lIs = lIs;
			this.dIs = dIs;
		}

		public void await() throws InterruptedException, ExecutionException {
			for (EdgeTask task : this.edgeTasks) {
				task.await();
			}
		}

	}

	public static class EdgeTask {

		protected final BaseChurnSim sim;
		public volatile IncrementalStats stats;

		public final int i;
		public final int j;

		private volatile Future<?> fFuture;

		public EdgeTask(BaseChurnSim sim, IncrementalStats stats, int i, int j) {
			this.sim = sim;
			this.i = i;
			this.j = j;
			this.stats = stats;
		}

		protected void setFuture(Future<?> future) {
			fFuture = future;
		}

		public void await() throws InterruptedException, ExecutionException {
			fFuture.get();
		}

	}
}
