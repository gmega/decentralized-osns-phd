package it.unitn.disi.churn.diffusion;

import it.unitn.disi.churn.config.ExperimentReader;
import it.unitn.disi.churn.config.ExperimentReader.Experiment;
import it.unitn.disi.churn.config.GraphConfigurator;
import it.unitn.disi.churn.diffusion.cloud.CloudAccessor;
import it.unitn.disi.churn.diffusion.cloud.CloudSimulationTask;
import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.distsim.scheduler.generators.IScheduleIterator;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.large.catalog.IGraphProvider;
import it.unitn.disi.newscasting.experiments.schedulers.SchedulerFactory;
import it.unitn.disi.simulator.TaskExecutor;
import it.unitn.disi.simulator.yao.YaoChurnConfigurator;
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.collections.Pair;
import it.unitn.disi.utils.streams.PrefixedWriter;
import it.unitn.disi.utils.tabular.TableWriter;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.IResolver;
import peersim.config.ObjectCreator;
import peersim.util.IncrementalStats;

@AutoConfig
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
		fExecutor = new TaskExecutor(fCores);
		IGraphProvider provider = fConfig.graphProvider();
		IScheduleIterator it = SchedulerFactory.getInstance()
				.createScheduler(fResolver, "").iterator();

		TableWriter writer = new TableWriter(new PrefixedWriter("ES:", oup),
				"id", "source", "target", "lsum");

		TableWriter cCore = new TableWriter(new PrefixedWriter("CO:", oup),
				"id", "source", "lsum", "size");

		TableWriter cloudStats = new TableWriter(
				new PrefixedWriter("CS:", oup), "id", "source", "accessed",
				"suppressed", "unfired");

		System.err.println("-- Simulation seeds are "
				+ (fFixSeed ? "fixed" : "variable") + ".");

		System.err.println("-- Cloud accessor clock type uses " + fClockType
				+ ".");

		int clockType = fClockType.equals("uptime") ? CloudAccessor.UPTIME
				: CloudAccessor.WALLCLOCK;

		IncrementalStats stats = new IncrementalStats();

		Integer row;
		while ((row = (Integer) it.nextIfAvailable()) != IScheduleIterator.DONE) {
			Experiment experiment = fReader.readExperiment(row, provider);

			IndexedNeighborGraph graph = provider.subgraph(experiment.root);
			int source = Integer.parseInt(experiment.attributes.get("node"));

			int[] ids = provider.verticesOf(experiment.root);
			Pair<int[], double[]> result = runExperiments(experiment,
					MiscUtils.indexOf(ids, source), ids, graph, fRepeats,
					cCore, clockType);

			int[] cloud = result.a;
			double[] latencies = result.b;

			if (fCloudAssisted) {
				cloudStats.set("id", experiment.root);
				cloudStats.set("source", source);
				cloudStats.set("accessed", cloud[CloudAccessor.ACCESSED]);
				cloudStats.set("suppressed", cloud[CloudAccessor.SUPPRESSED]);
				cloudStats.set("unfired", cloud[CloudAccessor.UNFIRED]);
				cloudStats.emmitRow();
			}

			if (fSummary) {
				stats.reset();
				for (int i = 0; i < latencies.length; i++) {
					stats.add(latencies[i]);
				}

				writer.set("id", experiment.root);
				writer.set("source", source);
				writer.set("target", stats.getN());
				writer.set("lsum", stats.getSum());
				writer.emmitRow();

			} else {
				for (int i = 0; i < latencies.length; i++) {
					writer.set("id", experiment.root);
					writer.set("source", source);
					writer.set("target", ids[i]);
					writer.set("lsum", latencies[i]);
					writer.emmitRow();
				}
			}
		}
	}

	private Pair<int[], double[]> runExperiments(Experiment experiment,
			int source, int[] ids, IndexedNeighborGraph graph, int repetitions,
			TableWriter core, int clockType) throws Exception {

		double[] latencies = new double[graph.size()];
		int[] counters = new int[3];
		fExecutor.start(experiment.toString() + ", source: " + source
				+ " size: " + graph.size(), repetitions);

		Random seedGen = null;
		if (fFixSeed) {
			seedGen = new Random(Long.parseLong(experiment.attributes
					.get("seed")));
		}

		for (int i = 0; i < repetitions; i++) {
			fExecutor.submit(createTask(experiment, source, graph,
					seedGen != null ? seedGen.nextLong() : null, clockType));
		}

		IncrementalStats stats = new IncrementalStats();

		for (int i = 0; i < repetitions; i++) {
			stats.reset();

			Object value = fExecutor.consume();
			if (value instanceof Throwable) {
				((Throwable) value).printStackTrace();
			} else {
				collectLatencies(source, value, latencies, stats);
				collectCloudAccesses(value, counters);
			}

			if (fYaoChurn != null) {
				core.set("id", experiment.root);
				core.set("source", ids[source]);
				core.set("lsum", stats.getSum());
				core.set("lsqrsum", stats.getSqrSum());
				core.set("size", stats.getN());
				core.emmitRow();
			}
		}

		return new Pair<int[], double[]>(counters, latencies);
	}

	private void collectLatencies(int source, Object value, double[] latencies,
			IncrementalStats stats) {
		HFlood[] result = ((ChurnSimulationTask) value).protocols();
		double base = result[source].latency();
		for (int j = 0; j < result.length; j++) {
			double latency = result[j].latency() - base;
			latencies[j] += latency;
			if (result[j].isPartOfConnectedCore()) {
				stats.add(latency);
			}
		}
	}

	private void collectCloudAccesses(Object value, int[] counters) {
		if (!fCloudAssisted) {
			return;
		}

		CloudAccessor[] accessors = ((CloudSimulationTask) value).accessors();
		for (int i = 0; i < accessors.length; i++) {
			counters[accessors[i].outcome()]++;
		}
	}

	private DiffusionSimulationTask createTask(Experiment experiment,
			int source, IndexedNeighborGraph graph, Long seed, int clockType) {

		Random rnd = new Random();

		// Static experiment.
		if (experiment.lis == null) {
			return new StaticSimulationTask(fBurnin, fPeriod, experiment,
					source, fSelector, graph, rnd);
		}

		if (fCloudAssisted) {
			return new CloudSimulationTask(fBurnin, fPeriod, experiment,
					fYaoChurn, source, fSelector, graph, rnd, fResolver, seed,
					clockType);
		} else {
			return new ChurnSimulationTask(fBurnin, fPeriod, experiment,
					fYaoChurn, source, fSelector, graph, rnd, seed);
		}
	}
}
