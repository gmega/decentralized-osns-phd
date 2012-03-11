package it.unitn.disi.churn.connectivity.wosn;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Future;

import org.lambda.functions.implementations.F0;

import it.unitn.disi.churn.IValueObserver;
import it.unitn.disi.churn.IncrementalStatsAdapter;
import it.unitn.disi.churn.YaoChurnConfigurator;
import it.unitn.disi.churn.connectivity.SimulationTask;
import it.unitn.disi.churn.intersync.ParallelParwiseSyncEstimator;
import it.unitn.disi.churn.intersync.ParallelParwiseSyncEstimator.EdgeTask;
import it.unitn.disi.churn.intersync.ParallelParwiseSyncEstimator.GraphTask;
import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.unitsim.ListGraphGenerator;
import it.unitn.disi.utils.CallbackThreadPoolExecutor;
import it.unitn.disi.utils.IExecutorCallback;
import it.unitn.disi.utils.logging.Progress;
import it.unitn.disi.utils.logging.ProgressTracker;
import it.unitn.disi.utils.tabular.TableWriter;
import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.IResolver;
import peersim.config.ObjectCreator;
import peersim.util.IncrementalStats;

@AutoConfig
public class WOSNLGExperiment implements ITransformer {

	@Attribute("repetitions")
	private int fRepetitions;

	@Attribute("n")
	private int fN;

	@Attribute("burnin")
	private double fBurnin;

	private YaoChurnConfigurator fYaoConf;

	private CallbackThreadPoolExecutor<double[]> fExecutor;

	private ProgressTracker fTracker;

	public WOSNLGExperiment(@Attribute(Attribute.AUTO) IResolver resolver) {
		fYaoConf = ObjectCreator.createInstance(YaoChurnConfigurator.class, "",
				resolver);

		fExecutor = new CallbackThreadPoolExecutor<double[]>(Runtime
				.getRuntime().availableProcessors(),
				new IExecutorCallback<double[]>() {
					@Override
					public void taskFailed(Future<double[]> task, Exception ex) {
						ex.printStackTrace();
						if (fTracker != null) {
							fTracker.tick();
						}
					}

					@Override
					public synchronized void taskDone(double[] result) {
						if (fTracker != null) {
							fTracker.tick();
						}
					}
				});

	}

	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {

		TableWriter writer = new TableWriter(new PrintStream(oup), "node",
				"estimate", "simulation");

		ListGraphGenerator lgg = new ListGraphGenerator();
		IndexedNeighborGraph graph = lgg.subgraph(fN);

		GraphTask gt = pairwiseEstimate(graph);

		gt.await();

		double[][] ld = new double[graph.size()][2];
		for (int i = 0; i < ld.length; i++) {
			ld[i][SimulationTask.LI] = gt.lIs[i];
			ld[i][SimulationTask.DI] = gt.dIs[i];
			System.out.println(ld[i][SimulationTask.LI] + " "
					+ ld[i][SimulationTask.DI]);
		}

		ArrayList<EdgeTask> edgeTasks = gt.edgeTasks;
		double[] estimation = new double[graph.size()];
		Arrays.fill(estimation, Double.POSITIVE_INFINITY);
		estimation[0] = 0;		
		int i = 0;
		for(EdgeTask task : edgeTasks) {
			if (i == (graph.size() - 1)) {
				break;
			}
			
			if (task.i == i && task.j == (i + 1)) {
				IncrementalStatsAdapter adapter = (IncrementalStatsAdapter) task.stats;
				estimation[i + 1] = estimation[i] + adapter.getStats().getAverage();
				i++;
			}
		}

		double[] simulation = simulate(graph, ld);

		for (i = 0; i < simulation.length; i++) {
			writer.set("node", i);
			writer.set("estimate", estimation[i]);
			writer.set("simulation", simulation[i] / fRepetitions);
			writer.emmitRow();
		}

		fExecutor.shutdown();
	}

	private GraphTask pairwiseEstimate(IndexedNeighborGraph graph) {
		ParallelParwiseSyncEstimator ppse = new ParallelParwiseSyncEstimator(
				fYaoConf, fRepetitions);

		return ppse.estimate(new F0<IValueObserver>() {
			{
				ret(new IncrementalStatsAdapter(new IncrementalStats()));
			};
		}, fExecutor, graph, 1);
	}

	private double[] simulate(IndexedNeighborGraph graph, double[][] ld)
			throws Exception {

		ArrayList<Future<double[]>> tasks = new ArrayList<Future<double[]>>();
		double[] ttc = new double[graph.size()];

		synchronized (fExecutor) {
			fTracker = Progress.newTracker("simulate", fRepetitions);
			fTracker.startTask();
		}

		for (int j = 0; j < fRepetitions; j++) {
			tasks.add(fExecutor.submit(new SimulationTask(ld, 0, fBurnin,
					graph, null, fYaoConf)));
		}

		for (Future<double[]> task : tasks) {
			double[] tce = task.get();
			for (int i = 0; i < ttc.length; i++) {
				ttc[i] += tce[i];
			}
		}

		return ttc;
	}

}
