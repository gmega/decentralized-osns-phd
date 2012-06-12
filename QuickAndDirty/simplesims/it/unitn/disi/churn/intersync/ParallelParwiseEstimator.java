package it.unitn.disi.churn.intersync;

import it.unitn.disi.simulator.core.EDSimulationEngine;
import it.unitn.disi.simulator.core.IProcess;
import it.unitn.disi.simulator.core.IProcess.State;
import it.unitn.disi.simulator.core.ISimulationObserver;
import it.unitn.disi.simulator.core.RenewalProcess;
import it.unitn.disi.simulator.measure.IValueObserver;
import it.unitn.disi.simulator.yao.YaoPresets.IDistributionGenerator;
import it.unitn.disi.utils.IExecutorCallback;
import it.unitn.disi.utils.collections.Pair;
import it.unitn.disi.utils.logging.IProgressTracker;
import it.unitn.disi.utils.logging.Progress;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.lambda.functions.Function0;

import peersim.graph.Graph;

public class ParallelParwiseEstimator implements IExecutorCallback<Object> {

	private IProgressTracker fTracker;

	private double fBurnin;

	// ------------------------------------------------------------------------

	public ParallelParwiseEstimator() {
		this(0);
	}

	public ParallelParwiseEstimator(double burnin) {
		fBurnin = burnin;
	}

	// ------------------------------------------------------------------------

	public GraphTask estimate(Function0<IValueObserver> oFactory,
			ExecutorService es, Graph g, int repetitions, double[] li,
			double[] di, IDistributionGenerator generator, boolean cloud,
			int id, int[] ids) {

		ArrayList<EdgeTask> tasks = new ArrayList<EdgeTask>();

		// Now estimates the TTC for all edges, in parallel.
		for (int j = 0; j < li.length; j++) {
			for (int k = 0; k < di.length; k++) {
				if (j == k) {
					continue;
				}

				if (g.isEdge(j, k)) {
					tasks.add(ttcTask(j, k, li, di, repetitions, cloud,
							generator, oFactory.call()));
				}
			}
		}

		initializeProgress(id, tasks.size());

		for (EdgeTask task : tasks) {
			synchronized (task) {
				task.setFuture(es.submit(task.sim));
			}
		}

		return new GraphTask(li, di, tasks);
	}

	// ------------------------------------------------------------------------

	public EdgeTask ttcTask(int i, int j, double[] lIs, double[] dIs,
			int repetitions, boolean cloud, IDistributionGenerator distGen,
			IValueObserver observer) {

		RenewalProcess pI = new RenewalProcess(i,
				distGen.uptimeDistribution(lIs[i]),
				distGen.downtimeDistribution(dIs[i]), State.down);

		RenewalProcess pJ = new RenewalProcess(j,
				distGen.uptimeDistribution(lIs[j]),
				distGen.downtimeDistribution(dIs[j]), State.down);

		ArrayList<Pair<Integer, ? extends ISimulationObserver>> sims = new ArrayList<Pair<Integer, ? extends ISimulationObserver>>();
		TrueSyncEstimator sexp = new TrueSyncEstimator(repetitions, cloud,
				observer);
		sims.add(new Pair<Integer, ISimulationObserver>(
				IProcess.PROCESS_SCHEDULABLE_TYPE, sexp));

		EDSimulationEngine churnSim = new EDSimulationEngine(new IProcess[] { pI, pJ }, sims,
				fBurnin);

		return new EdgeTask(churnSim, observer, i, j);
	}

	// ------------------------------------------------------------------------

	private synchronized void initializeProgress(int sampleId, int size) {
		fTracker = Progress.newTracker("est. TTC sample " + sampleId + " ("
				+ size + ")", size);
		fTracker.startTask();
	}

	// ------------------------------------------------------------------------

	@Override
	public synchronized void taskFailed(Future<Object> task, Throwable ex) {
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

		protected final EDSimulationEngine sim;
		public volatile IValueObserver stats;

		public final int i;
		public final int j;

		private volatile Future<?> fFuture;

		public EdgeTask(EDSimulationEngine sim, IValueObserver stats, int i, int j) {
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
