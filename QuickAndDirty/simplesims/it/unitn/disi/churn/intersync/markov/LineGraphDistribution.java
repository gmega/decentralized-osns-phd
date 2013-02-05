package it.unitn.disi.churn.intersync.markov;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Random;

import jphase.ContPhaseVar;
import jphase.DenseContPhaseVar;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.IResolver;
import peersim.config.ObjectCreator;
import it.unitn.disi.churn.connectivity.SimulationTaskBuilder;
import it.unitn.disi.churn.diffusion.experiments.config.Utils;
import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.simulator.churnmodel.yao.YaoChurnConfigurator;
import it.unitn.disi.simulator.churnmodel.yao.YaoPresets.IAverageGenerator;
import it.unitn.disi.simulator.concurrent.SimulationTask;
import it.unitn.disi.simulator.concurrent.TaskExecutor;
import it.unitn.disi.simulator.measure.INodeMetric;
import it.unitn.disi.unitsim.ListGraphGenerator;
import it.unitn.disi.utils.streams.PrefixedWriter;
import it.unitn.disi.utils.tabular.TableWriter;

@AutoConfig
public class LineGraphDistribution implements ITransformer {

	@Attribute("repetitions")
	private int fRepetitions;

	@Attribute("n")
	private int fN;

	@Attribute("burnin")
	private double fBurnin;

	@Attribute("cores")
	private int fCores;

	@Attribute("pdfonly")
	private boolean fPDFOnly;
	
	@Attribute("experiments")
	private int fExperiments;

	private YaoChurnConfigurator fYaoConf;

	public LineGraphDistribution(@Attribute(Attribute.AUTO) IResolver resolver) {
		fYaoConf = ObjectCreator.createInstance(YaoChurnConfigurator.class, "",
				resolver);
	}

	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {
		TaskExecutor taskExecutor = new TaskExecutor(fCores);
		ListGraphGenerator lgg = new ListGraphGenerator();
		IndexedNeighborGraph graph = lgg.subgraph(fN);

		TableWriter writer = new TableWriter(oup, "ed", "esum", "phase");
		
		for (int i = 0; i < fExperiments; i++) {
			runExperiment(taskExecutor, graph, writer);	
		}
		
		taskExecutor.shutdown(true);
	}

	private void runExperiment(TaskExecutor executor,
			IndexedNeighborGraph graph, TableWriter writer) throws Exception {
		double[] li = new double[graph.size()];
		double[] di = new double[graph.size()];
		IAverageGenerator gen = fYaoConf.averageGenerator(new Random());

		for (int i = 0; i < graph.size(); i++) {
			li[i] = gen.nextLI();
			di[i] = gen.nextDI();
			//System.out.println("S:" + i + " " + li[i] + " " + di[i]);
		}

		double ed = fPDFOnly ? -1 : simulate(graph, li, di, executor);
		double[] estimate = estimate(graph, li, di);
		PhaseTypeDistribution pdt = phasePDF(li, di);

		writer.set("ed", ed);
		writer.set("esum", estimate[estimate.length - 1]);
		writer.set("phase", pdt.expectation());
		writer.emmitRow();
	}

	private PhaseTypeDistribution phasePDF(double[] li, double[] di) {
		PhaseTypeDistribution pathDelay = null;
		for (int i = 0; i < (li.length - 1); i++) {
			PhaseTypeDistribution edgeDelay = new PhaseTypeDistribution(
					genMatrix(1.0 / li[i], 1.0 / li[i + 1], 2.0 / di[i],
							2.0 / di[i + 1]), alpha(1.0 / li[i],
							1.0 / li[i + 1], 2.0 / di[i], 2.0 / di[i + 1]));
			pathDelay = pathDelay == null ? edgeDelay : pathDelay
					.sum(edgeDelay);
		}
		return pathDelay;
	}

	private double[][] genMatrix(double mu_u, double mu_v, double gamma_u,
			double gamma_v) {
		return new double[][] { { -(gamma_u + gamma_v), gamma_v, gamma_u, 0 },
				{ mu_v, -(gamma_u + mu_v), 0, gamma_u },
				{ mu_u, 0, -(mu_u + gamma_v), gamma_v }, { 0, 0, 0, 0 } };
	}

	private double[] alpha(double mu_u, double mu_v, double gamma_u,
			double gamma_v) {

		// Alpha zero is the stable state probability of v being online.
		double pi_zero = mu_v / (mu_v + gamma_v);

		return new double[] { 0.0, 0.0, pi_zero, 1 - pi_zero };
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

	private double simulate(IndexedNeighborGraph graph, double[] lis,
			double[] dis, TaskExecutor executor) throws Exception {

		int ids[] = new int[graph.size()];
		for (int i = 0; i < ids.length; i++) {
			ids[i] = i;
		}

		executor.start("simulate", fRepetitions);

		for (int j = 0; j < fRepetitions; j++) {
			SimulationTaskBuilder builder = new SimulationTaskBuilder(graph, 0,
					lis, dis, fYaoConf);
			builder.addConnectivitySimulation(0, "ed", "rd");
			executor.submit(builder.simulationTask(fBurnin));
		}

		double edval = 0;
		for (int j = 0; j < fRepetitions; j++) {
			SimulationTask task = (SimulationTask) executor.consume();
			List<? extends INodeMetric<?>> result = task.metric(0);

			INodeMetric<Double> ed = Utils.lookup(result, "ed", Double.class);
			// INodeMetric<Double> rd = Utils.lookup(result, "rd",
			// Double.class);

			edval += ed.getMetric(graph.size() - 1);
		}

		return edval / fRepetitions;
	}

}
