package it.unitn.disi.churn.intersync.markov;

import it.unitn.disi.churn.connectivity.SimulationTaskBuilder;
import it.unitn.disi.churn.diffusion.experiments.config.Utils;
import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.generators.ListGraphGenerator;
import it.unitn.disi.simulator.churnmodel.yao.YaoChurnConfigurator;
import it.unitn.disi.simulator.churnmodel.yao.YaoPresets.IAverageGenerator;
import it.unitn.disi.simulator.concurrent.SimulationTask;
import it.unitn.disi.simulator.concurrent.TaskExecutor;
import it.unitn.disi.simulator.measure.INodeMetric;
import it.unitn.disi.utils.streams.PrefixedWriter;
import it.unitn.disi.utils.tabular.TableWriter;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Random;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.IResolver;
import peersim.config.ObjectCreator;

/**
 * Experiment for verifying that the Markov model holds in line graphs.
 * 
 * @author giuliano
 */
@AutoConfig
public class LineGraphAveragesExperiment implements ITransformer {

	@Attribute("repetitions")
	private int fRepetitions;

	@Attribute("n")
	private int fN;

	@Attribute("experiments")
	private int fExperiments;

	@Attribute("burnin")
	private double fBurnin;

	@Attribute("cores")
	private int fCores;

	private YaoChurnConfigurator fYaoConf;

	public LineGraphAveragesExperiment(@Attribute(Attribute.AUTO) IResolver resolver) {
		fYaoConf = ObjectCreator.createInstance(YaoChurnConfigurator.class, "",
				resolver);
	}

	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {

		TableWriter writer = new TableWriter(new PrefixedWriter("RES:", oup),
				"node", "estimate", "simulation");
		TaskExecutor taskExecutor = new TaskExecutor(fCores);
		ListGraphGenerator lgg = new ListGraphGenerator();
		IndexedNeighborGraph graph = lgg.subgraph(fN);

		for (int i = 0; i < fExperiments; i++) {
			runExperiment(taskExecutor, graph, writer);
		}

		taskExecutor.shutdown(true);
	}

	private void runExperiment(TaskExecutor executor,
			IndexedNeighborGraph graph, TableWriter out) throws Exception {
		double[] li = new double[graph.size()];
		double[] di = new double[graph.size()];
		IAverageGenerator gen = fYaoConf.averageGenerator(new Random());

		for (int i = 0; i < graph.size(); i++) {
			li[i] = gen.nextLI();
			di[i] = gen.nextDI();
			System.out.println("S:" + i + " " + li[i] + " " + di[i]);
		}

		double[] simulation = simulate(graph, li, di, executor);
		double[] estimation = estimate(graph, li, di);

		for (int i = 0; i < simulation.length; i++) {
			out.set("node", i);
			out.set("estimate", estimation[i]);
			out.set("simulation", simulation[i] / fRepetitions);
			out.emmitRow();
		}

	}

	private double[] estimate(IndexedNeighborGraph graph, double[] li,
			double[] di) {
		double[] estimates = new double[graph.size()];

		for (int i = 1; i < estimates.length; i++) {
			estimates[i] = estimates[i - 1]
					+ edgeDelay(1.0 / li[i - 1], 1.0 / li[i], 2.0 / di[i - 1],
							2.0 / di[i]);
		}

		return estimates;
	}

	private double edgeDelay(double lu, double lv, double du, double dv) {
		double firstHitting = ((du + lu) * (du + dv + lv))
				/ (du * dv * (du + lu + dv + lv));
		double stableState = (lv) / (lv + dv);

		return stableState * firstHitting;
	}

	private double[] simulate(IndexedNeighborGraph graph, double[] lis,
			double[] dis, TaskExecutor executor) throws Exception {

		int ids[] = new int[graph.size()];
		for (int i = 0; i < ids.length; i++) {
			ids[i] = i;
		}

		double[] ttc = new double[graph.size()];

		executor.start("simulate", fRepetitions);

		for (int j = 0; j < fRepetitions; j++) {
			SimulationTaskBuilder builder = new SimulationTaskBuilder(graph, 0,
					lis, dis, fYaoConf);
			builder.addConnectivitySimulation(0, "ed", "rd");
			executor.submit(builder.simulationTask(fBurnin));
		}

		for (int j = 0; j < fRepetitions; j++) {
			SimulationTask task = (SimulationTask) executor.consume();
			List<? extends INodeMetric<?>> result = task.metric(0);
			INodeMetric<Double> ed = Utils.lookup(result, "ed", Double.class);
			for (int i = 0; i < ttc.length; i++) {
				ttc[i] += ed.getMetric(i);
			}
		}

		return ttc;
	}
}
