package it.unitn.disi.churn.connectivity.p2p;

import it.unitn.disi.churn.connectivity.SimulationTaskBuilder;
import it.unitn.disi.churn.intersync.ParallelParwiseEstimator;
import it.unitn.disi.churn.intersync.ParallelParwiseEstimator.EdgeTask;
import it.unitn.disi.churn.intersync.ParallelParwiseEstimator.GraphTask;
import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.simulator.INetworkMetric;
import it.unitn.disi.simulator.IValueObserver;
import it.unitn.disi.simulator.IncrementalStatsAdapter;
import it.unitn.disi.simulator.TaskExecutor;
import it.unitn.disi.simulator.yao.YaoChurnConfigurator;
import it.unitn.disi.simulator.yao.YaoPresets.IAverageGenerator;
import it.unitn.disi.unitsim.ListGraphGenerator;
import it.unitn.disi.utils.collections.Pair;
import it.unitn.disi.utils.tabular.TableWriter;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.lambda.functions.implementations.F0;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.util.IncrementalStats;

@AutoConfig
public class P2PLGExperiment implements ITransformer {

	@Attribute("repetitions")
	private int fRepetitions;

	@Attribute("n")
	private int fN;

	@Attribute("burnin")
	private double fBurnin;

	@Attribute("cores")
	private int fCores;

	private YaoChurnConfigurator fYaoConf;

	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {

		TableWriter writer = new TableWriter(new PrintStream(oup), "node",
				"estimate", "simulation");

		ListGraphGenerator lgg = new ListGraphGenerator();
		IndexedNeighborGraph graph = lgg.subgraph(fN);

		ExecutorService es = Executors.newScheduledThreadPool(fCores);
		
		GraphTask gt = pairwiseEstimate(es, graph);

		gt.await();

		ArrayList<EdgeTask> edgeTasks = gt.edgeTasks;
		double[] estimation = new double[graph.size()];
		Arrays.fill(estimation, Double.POSITIVE_INFINITY);
		estimation[0] = 0;
		int i = 0;
		for (EdgeTask task : edgeTasks) {
			if (i == (graph.size() - 1)) {
				break;
			}

			if (task.i == i && task.j == (i + 1)) {
				IncrementalStatsAdapter adapter = (IncrementalStatsAdapter) task.stats;
				estimation[i + 1] = estimation[i]
						+ adapter.getStats().getAverage();
				i++;
			}
		}
		
		es.shutdown();

		double[] simulation = simulate(graph, gt.lIs, gt.dIs);

		for (i = 0; i < simulation.length; i++) {
			writer.set("node", i);
			writer.set("estimate", estimation[i]);
			writer.set("simulation", simulation[i] / fRepetitions);
			writer.emmitRow();
		}
	}

	private GraphTask pairwiseEstimate(ExecutorService es,
			IndexedNeighborGraph graph) {

		ParallelParwiseEstimator ppse = new ParallelParwiseEstimator();

		double[] li = new double[graph.size()];
		double[] di = new double[graph.size()];
		IAverageGenerator gen = fYaoConf.averageGenerator();

		for (int i = 0; i < graph.size(); i++) {
			li[i] = gen.nextLI();
			di[i] = gen.nextDI();
			System.out.println(i + " " + li[i] + " " + +di[i]);
		}

		return ppse.estimate(new F0<IValueObserver>() {
			{
				ret(new IncrementalStatsAdapter(new IncrementalStats()));
			};
		}, es, graph, fRepetitions, li, di, fYaoConf.distributionGenerator(),
				false, 1, null);
	}

	private double[] simulate(IndexedNeighborGraph graph, double[] lis,
			double[] dis) throws Exception {

		int ids[] = new int[graph.size()];
		for (int i = 0; i < ids.length; i++) {
			ids[i] = i;
		}

		double[] ttc = new double[graph.size()];

		TaskExecutor taskExecutor = new TaskExecutor(fCores);

		taskExecutor.start("simulate", fRepetitions);

		for (int j = 0; j < fRepetitions; j++) {
			SimulationTaskBuilder builder = new SimulationTaskBuilder(graph,
					ids, 0);
			builder.addConnectivitySimulation(0, new int[] {}, null);
			taskExecutor.submit(builder.simulationTask(lis, dis, fBurnin,
					fYaoConf));
		}

		for (int j = 0; j < fRepetitions; j++) {
			@SuppressWarnings("unchecked")
			Pair<Integer, List<? extends INetworkMetric>> result = ((Pair<Integer, List<? extends INetworkMetric>>[]) taskExecutor
					.consume())[0];
			INetworkMetric ed = Utils.lookup(result.b, "ed");
			for (int i = 0; i < ttc.length; i++) {
				ttc[i] += ed.getMetric(i);
			}
		}
		
		taskExecutor.shutdown(true);

		return ttc;
	}
}
