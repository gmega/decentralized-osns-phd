package it.unitn.disi.churn.diffusion;

import it.unitn.disi.churn.config.ExperimentReader;
import it.unitn.disi.churn.config.ExperimentReader.Experiment;
import it.unitn.disi.churn.config.GraphConfigurator;
import it.unitn.disi.churn.connectivity.p2p.Utils;
import it.unitn.disi.churn.diffusion.cloud.CloudAccessor;
import it.unitn.disi.churn.diffusion.config.ChurnSimulationBuilder;
import it.unitn.disi.churn.diffusion.config.CloudSimulationBuilder;
import it.unitn.disi.churn.diffusion.config.StaticSimulationBuilder;
import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.distsim.scheduler.generators.IScheduleIterator;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.large.catalog.IGraphProvider;
import it.unitn.disi.newscasting.experiments.schedulers.SchedulerFactory;
import it.unitn.disi.simulator.concurrent.SimulationTask;
import it.unitn.disi.simulator.concurrent.TaskExecutor;
import it.unitn.disi.simulator.core.EDSimulationEngine;
import it.unitn.disi.simulator.core.IProcess;
import it.unitn.disi.simulator.measure.IMetricAccumulator;
import it.unitn.disi.simulator.measure.INodeMetric;
import it.unitn.disi.simulator.measure.MetricsCollector;
import it.unitn.disi.simulator.measure.SumAccumulation;
import it.unitn.disi.simulator.yao.YaoChurnConfigurator;
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.collections.Pair;
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
import peersim.util.IncrementalStats;

@AutoConfig
@SuppressWarnings("unchecked")
public class DiffusionExperiment implements ITransformer {

	private GraphConfigurator fConfig;

	@Attribute("period")
	private double fPeriod;

	@Attribute("burnin")
	private double fBurnin;

	@Attribute("selector")
	private String fSelector;

	@Attribute("repeats")
	private int fRepeats;

	@Attribute("cores")
	private int fCores;

	@Attribute(value = "summaryonly", defaultValue = "false")
	private boolean fSummary;

	@Attribute(value = "cloudassisted", defaultValue = "false")
	private boolean fCloudAssisted;

	@Attribute(value = "fixedseeds", defaultValue = "false")
	private boolean fFixSeed;

	@Attribute(value = "clocktype", defaultValue = "uptime")
	private String fClockType;

	private YaoChurnConfigurator fYaoChurn;

	private IResolver fResolver;

	private ExperimentReader fReader;

	private TaskExecutor fExecutor;

	public DiffusionExperiment(@Attribute(Attribute.AUTO) IResolver resolver,
			@Attribute(value = "churn", defaultValue = "false") boolean churn) {
		fConfig = ObjectCreator.createInstance(GraphConfigurator.class, "",
				resolver);

		if (churn) {
			fYaoChurn = ObjectCreator.createInstance(
					YaoChurnConfigurator.class, "", resolver);
		}

		fReader = new ExperimentReader("id");
		ObjectCreator
				.fieldInject(ExperimentReader.class, fReader, "", resolver);

		fResolver = resolver;
	}

	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {
		fExecutor = new TaskExecutor(fCores, 10);
		IGraphProvider provider = fConfig.graphProvider();
		IScheduleIterator it = SchedulerFactory.getInstance()
				.createScheduler(fResolver, "").iterator();

		TableWriter writer = new TableWriter(new PrefixedWriter("ES:", oup),
				"id", "source", "target", "edsum", "rdsum", "size");

		TableWriter cCore = new TableWriter(new PrefixedWriter("CO:", oup),
				"id", "source", "edsum", "size");

		TableWriter cloudStats = new TableWriter(
				new PrefixedWriter("CS:", oup), "id", "source", "accessed",
				"suppressed", "unfired");

		System.err.println("-- Simulation seeds are "
				+ (fFixSeed ? "fixed" : "variable") + ".");

		System.err.println("-- Cloud accessor clock type uses " + fClockType
				+ ".");

		int clockType = fClockType.equals("uptime") ? CloudAccessor.UPTIME
				: CloudAccessor.WALLCLOCK;

		Integer row;
		while ((row = (Integer) it.nextIfAvailable()) != IScheduleIterator.DONE) {
			Experiment experiment = fReader.readExperiment(row, provider);

			IndexedNeighborGraph graph = provider.subgraph(experiment.root);
			int source = Integer.parseInt(experiment.attributes.get("node"));

			int[] ids = provider.verticesOf(experiment.root);
			MetricsCollector result = runExperiments(experiment,
					MiscUtils.indexOf(ids, source), ids, graph, fRepeats,
					cCore, clockType);

			if (fCloudAssisted) {
				INodeMetric<Integer> accesses = result.getMetric("outcomes");

				cloudStats.set("id", experiment.root);
				cloudStats.set("source", source);
				cloudStats.set("accessed",
						accesses.getMetric(CloudAccessor.ACCESSED));
				cloudStats.set("suppressed",
						accesses.getMetric(CloudAccessor.SUPPRESSED));
				cloudStats.set("unfired",
						accesses.getMetric(CloudAccessor.UNFIRED));
				cloudStats.emmitRow();
			}

			INodeMetric<Double> ed = result.getMetric("ed");
			INodeMetric<Double> rd = result.getMetric("rd");

			if (fSummary) {
				IncrementalStats edSumm = summarize(ed, graph.size());
				IncrementalStats rdSumm = summarize(rd, graph.size());

				writer.set("id", experiment.root);
				writer.set("source", source);
				writer.set("target", "none");
				writer.set("edsum", edSumm.getSum());
				writer.set("rdsum", rdSumm.getSum());
				writer.set("size", graph.size());

				writer.emmitRow();

			} else {
				for (int i = 0; i < graph.size(); i++) {
					writer.set("id", experiment.root);
					writer.set("source", source);
					writer.set("target", ids[i]);
					writer.set("edsum", ed.getMetric(i));
					writer.set("rdsum", rd.getMetric(i));
					writer.set("size", graph.size());

					writer.emmitRow();
				}
			}
		}
	}

	private IncrementalStats summarize(INodeMetric<Double> ed, int size) {
		IncrementalStats summary = new IncrementalStats();
		for (int i = 0; i < size; i++) {
			summary.add(ed.getMetric(i));
		}
		return summary;
	}

	private MetricsCollector runExperiments(final Experiment experiment,
			final int source, int[] ids, final IndexedNeighborGraph graph,
			final int repetitions, TableWriter core, final int clockType)
			throws Exception {

		fExecutor.start(experiment.toString() + ", source: " + source
				+ " size: " + graph.size(), repetitions);

		final Random seedGen = fFixSeed ? new Random(
				Long.parseLong(experiment.attributes.get("seed"))) : null;

		Thread submitter = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					for (int i = 0; i < repetitions; i++) {
						fExecutor.submit(createTask(experiment, source, graph,
								seedGen != null ? seedGen.nextLong() : null,
								clockType));
					}
				} catch (Exception ex) {
					System.err
							.println("Submission thread finished with exception.");
					ex.printStackTrace();
				}
			}
		});
		
		submitter.start();

		SumAccumulation edsum = new SumAccumulation("ed", graph.size());
		SumAccumulation rdsum = new SumAccumulation("rd", graph.size());

		MetricsCollector collector = new MetricsCollector(
				new CloudAccessCounter(graph.size()), edsum, rdsum);

		for (int i = 0; i < repetitions; i++) {
			Object value = fExecutor.consume();
			if (isThrowable(value)) {
				continue;
			}

			SimulationTask result = (SimulationTask) value;
			List<? extends INodeMetric<?>> metrics = result.metric(source);
			collector.add(metrics);

			printCoreLatencies(experiment.root, source, graph.size(),
					Utils.lookup(metrics, "coremembership", Boolean.class),
					Utils.lookup(metrics, "ed", Double.class), core);
		}

		return collector;
	}

	private void printCoreLatencies(int id, int source, int size,
			INodeMetric<Boolean> membership, INodeMetric<Double> ed,
			TableWriter core) {

		if (membership == null) {
			return;
		}

		IncrementalStats edStats = new IncrementalStats();

		int cardinality = 0;
		for (int i = 0; i < size; i++) {
			if (membership.getMetric(i)) {
				edStats.add(ed.getMetric(i));
				cardinality++;
			}
		}

		core.set("id", id);
		core.set("source", source);
		core.set("edsum", edStats.getAverage());
		core.set("size", cardinality);
		core.emmitRow();
	}

	private boolean isThrowable(Object value) {
		if (value instanceof Throwable) {
			((Throwable) value).printStackTrace();
			return true;
		}
		return false;
	}

	private SimulationTask createTask(Experiment experiment, int source,
			IndexedNeighborGraph graph, Long seed, int clockType)
			throws Exception {

		Random rnd = new Random();
		Pair<EDSimulationEngine, List<INodeMetric<? extends Object>>> elements;

		// Static experiment.
		if (experiment.lis == null) {
			StaticSimulationBuilder builder = new StaticSimulationBuilder();
			elements = builder.build(fBurnin, fPeriod, experiment, source,
					fSelector, graph, rnd);
		} else {
			IProcess[] processes = fYaoChurn.createProcesses(experiment.lis,
					experiment.dis, graph.size(), seed != null ? new Random(
							seed) : new Random());

			if (fCloudAssisted) {
				elements = new CloudSimulationBuilder().build(fBurnin, fPeriod,
						experiment, source, fSelector, graph, rnd, fResolver,
						clockType, processes);
			} else {
				elements = new ChurnSimulationBuilder().build(fBurnin, fPeriod,
						experiment, source, fSelector, graph, rnd, processes);
			}
		}

		return new SimulationTask(
				null,
				elements.a,
				new Pair[] { new Pair<Integer, List<INodeMetric<? extends Object>>>(
						source, elements.b) });
	}

	private static class CloudAccessCounter implements
			IMetricAccumulator<Integer> {

		private final int fN;

		private final int[] fCounter = new int[3];

		public CloudAccessCounter(int n) {
			fN = n;
		}

		@Override
		public Object id() {
			return "outcome";
		}

		@Override
		public Integer getMetric(int i) {
			return fCounter[i];
		}

		@Override
		public void add(INodeMetric<Integer> metric) {
			for (int i = 0; i < fN; i++) {
				fCounter[metric.getMetric(i)]++;
			}
		}

	}
}
