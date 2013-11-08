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
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.tabular.TableWriter;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Random;

import jphase.DenseContPhaseVar;
import jphase.generator.NeutsContPHGenerator;
import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.IResolver;
import peersim.config.ObjectCreator;

@AutoConfig
public class LineGraphDistributionExperiment implements ITransformer {

	private static final int AVG = 0;
	private static final int MAX = 1;

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

	@Attribute(value = "printdist", defaultValue = "false")
	private boolean fPrintDistribution;

	@Attribute("experiments")
	private int fExperiments;

	private YaoChurnConfigurator fYaoConf;

	public LineGraphDistributionExperiment(
			@Attribute(Attribute.AUTO) IResolver resolver) {
		fYaoConf = ObjectCreator.createInstance(YaoChurnConfigurator.class, "",
				resolver);
	}

	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {
		TaskExecutor taskExecutor = new TaskExecutor(fCores, 10000);
		ListGraphGenerator lgg = new ListGraphGenerator();
		IndexedNeighborGraph graph = lgg.subgraph(fN);

		TableWriter writer = new TableWriter(oup, "path", "ed", "esum",
				"phase", "jphase");

		for (int i = 0; i < fExperiments; i++) {
			runExperiment(taskExecutor, i, graph, writer);
		}

		taskExecutor.shutdown(true);
	}

	private void runExperiment(TaskExecutor executor, int id,
			IndexedNeighborGraph graph, TableWriter writer) throws Exception {
		double[] li = new double[graph.size()];
		double[] di = new double[graph.size()];
		IAverageGenerator gen = fYaoConf.averageGenerator(new Random());

		for (int i = 0; i < graph.size(); i++) {
			li[i] = gen.nextLI();
			di[i] = gen.nextDI();
		}

		PhaseTypeDistribution pdt = phasePDF(li, di);
		DenseContPhaseVar jPhasePDF = pdt.getJPhaseDistribution();

		double[] result = fPDFOnly ? null : simulate(graph,
				new NeutsContPHGenerator(jPhasePDF), li, di, id, executor);
		double[] estimate = estimate(graph, li, di);

		writer.set("path", id);
		writer.set("ed", result[AVG]);
		writer.set("esum", estimate[estimate.length - 1]);
		writer.set("phase", pdt.expectation());
		writer.set("jphase", pdt.getJPhaseDistribution().moment(1));
		writer.emmitRow();

		if (fPrintDistribution) {
			printDistribution(id, result[MAX], jPhasePDF);
		}
	}

	private void printDistribution(int id, double d, DenseContPhaseVar pdf) {
		System.out.println("PDF:id x y");
		for (double x = 0.0; x < d; x += 0.1) {
			System.out.println("PDF:" + id + " " + x + " " + pdf.pdf(x));
		}
	}

	private PhaseTypeDistribution phasePDF(double[] li, double[] di) {
		PhaseTypeDistribution pathDelay = null;
		for (int i = 0; i < (li.length - 1); i++) {
			PhaseTypeDistribution edgeDelay = new PhaseTypeDistribution(
					MarkovDelayModel.genMatrix(1.0 / li[i], 1.0 / li[i + 1],
							1.0 / di[i], 1.0 / di[i + 1]),
					MarkovDelayModel.alpha(1.0 / li[i], 1.0 / li[i + 1],
							1.0 / di[i], 1.0 / di[i + 1]));
			pathDelay = pathDelay == null ? edgeDelay : pathDelay
					.sum(edgeDelay);
		}
		return pathDelay;
	}

	private double[] estimate(IndexedNeighborGraph graph, double[] li,
			double[] di) {
		double[] estimates = new double[graph.size()];

		for (int i = 1; i < estimates.length; i++) {
			estimates[i] = estimates[i - 1]
					+ edgeDelay(1.0 / li[i - 1], 1.0 / li[i], 1.0 / di[i - 1],
							1.0 / di[i]);
		}

		return estimates;
	}

	private double edgeDelay(double lu, double lv, double du, double dv) {
		double firstHitting = ((du + lu) * (du + dv + lv))
				/ (du * dv * (du + lu + dv + lv));
		double stableState = (lv) / (lv + dv);
		return stableState * firstHitting;
	}

	private double[] simulate(final IndexedNeighborGraph graph,
			NeutsContPHGenerator generator, final double[] lis,
			final double[] dis, int id, final TaskExecutor executor)
			throws Exception {

		int ids[] = new int[graph.size()];
		for (int i = 0; i < ids.length; i++) {
			ids[i] = i;
		}

		executor.start("simulate", fRepetitions);

		Thread submitter = new Thread(new Runnable() {
			public void run() {
				for (int j = 0; j < fRepetitions; j++) {
					SimulationTaskBuilder builder = new SimulationTaskBuilder(
							graph, 0, lis, dis, fYaoConf);
					builder.addConnectivitySimulation(0, "ed", "rd");
					try {
						executor.submit(builder.simulationTask(fBurnin));
					} catch (Exception ex) {
						throw MiscUtils.nestRuntimeException(ex);
					}
				}
			}
		});
		submitter.start();

		double edsum = 0;
		double max = Double.MIN_VALUE;
		System.out.println("SP:path simpoint phasepoint");
		for (int j = 0; j < fRepetitions; j++) {
			SimulationTask task = (SimulationTask) executor.consume();
			List<? extends INodeMetric<?>> result = task.metric(0);

			INodeMetric<Double> ed = Utils.lookup(result, "ed", Double.class);
			double simedpoint = ed.getMetric(graph.size() - 1);
			double phasedpoint = generator.getRandom();

			StringBuffer sbuffer = new StringBuffer("SP:");
			if (fPrintDistribution) {
				sbuffer.append(id);
				sbuffer.append(" ");
				sbuffer.append(simedpoint);
				sbuffer.append(" ");
				sbuffer.append(phasedpoint);
				System.out.println(sbuffer);
			}

			max = Math.max(simedpoint, max);
			edsum += simedpoint;
		}

		return new double[] { (edsum / fRepetitions), max };
	}

}
