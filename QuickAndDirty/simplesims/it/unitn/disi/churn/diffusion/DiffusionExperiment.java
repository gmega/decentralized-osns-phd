package it.unitn.disi.churn.diffusion;

import gnu.trove.list.array.TIntArrayList;
import it.unitn.disi.churn.config.ExperimentReader;
import it.unitn.disi.churn.config.ExperimentReader.Experiment;
import it.unitn.disi.churn.config.GraphConfigurator;
import it.unitn.disi.churn.connectivity.p2p.Utils;
import it.unitn.disi.churn.diffusion.cloud.SimpleCloudImpl;
import it.unitn.disi.churn.diffusion.config.ChurnSimulationBuilder;
import it.unitn.disi.churn.diffusion.config.CloudSimulationBuilder;
import it.unitn.disi.churn.diffusion.config.StaticSimulationBuilder;
import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.distsim.scheduler.generators.IScheduleIterator;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.large.catalog.IGraphProvider;
import it.unitn.disi.newscasting.experiments.schedulers.SchedulerFactory;
import it.unitn.disi.simulator.churnmodel.yao.YaoChurnConfigurator;
import it.unitn.disi.simulator.concurrent.SimulationTask;
import it.unitn.disi.simulator.concurrent.TaskExecutor;
import it.unitn.disi.simulator.core.EDSimulationEngine;
import it.unitn.disi.simulator.core.IProcess;
import it.unitn.disi.simulator.core.IProcess.State;
import it.unitn.disi.simulator.measure.INodeMetric;
import it.unitn.disi.simulator.measure.MetricsCollector;
import it.unitn.disi.simulator.measure.SumAccumulation;
import it.unitn.disi.simulator.protocol.FixedProcess;
import it.unitn.disi.simulator.random.SimulationTaskException;
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.collections.Pair;
import it.unitn.disi.utils.streams.PrefixedWriter;
import it.unitn.disi.utils.tabular.TableWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.util.BitSet;
import java.util.HashMap;
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

	@Attribute(value = "access_delay", defaultValue = "0")
	private double fDelay;

	@Attribute(value = "nup_anchor", defaultValue = "-1")
	private double fNUPAnchor;

	@Attribute(value = "nup_burnin", defaultValue = "-1")
	private double fNUPBurnin;

	@Attribute(value = "fixed_node_map", defaultValue = "none")
	private String fFixedMap;

	private YaoChurnConfigurator fYaoChurn;

	private IResolver fResolver;

	private ExperimentReader fReader;

	private TaskExecutor fExecutor;

	private volatile int fSeedUniquefier;

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
				"id", "source", "target", "edsum", "rdsum", "size", "fixed",
				"exps");

		TableWriter cCore = new TableWriter(new PrefixedWriter("CO:", oup),
				"id", "source", "edsum", "size");

		TableWriter cloudStats = new TableWriter(
				new PrefixedWriter("CS:", oup), "id", "source", "target",
				"accup", "accnup", "exps");

		System.err.println("-- Simulation seeds are "
				+ (fFixSeed ? "fixed" : "variable") + ".");

		BitSet fixedMap = fixedNodeMap();
		System.err.print("There are " + fixedMap.cardinality() + " fixed nodes.");

		Integer row;
		while ((row = (Integer) it.nextIfAvailable()) != IScheduleIterator.DONE) {
			Experiment experiment = fReader.readExperiment(row, provider);
			IndexedNeighborGraph graph = provider.subgraph(experiment.root);
			int source = Integer.parseInt(experiment.attributes.get("node"));

			int[] ids = provider.verticesOf(experiment.root);
			int [] fixed = cloudNodes(experiment, ids, fixedMap);
			MetricsCollector result = runExperiments(experiment,
					MiscUtils.indexOf(ids, source), ids, graph, fRepeats,
					cCore, fixed);

			if (fCloudAssisted) {
				printCloudMetrics(cloudStats, experiment, source, ids, result);
			}

			if (fNUPAnchor < 0) {
				printLatencyMetrics(writer, experiment, graph, source, ids,
						result, fixed);
			}
		}
	}

	private int[] cloudNodes(Experiment exp, int[] ids, BitSet fixed)
			throws Exception {

		// Reads cloud nodes.
		TIntArrayList cloud = new TIntArrayList();
		for (int i = 0; i < ids.length; i++) {
			int row = exp.entry.rowStart + i;
			if (fixed.get(row)) {
				cloud.add(i);
			}
		}

		return cloud.toArray();

	}

	@SuppressWarnings("resource")
	private BitSet fixedNodeMap() throws Exception {
		if (fFixedMap.equals("none")) {
			return new BitSet();
		}

		return (BitSet) new ObjectInputStream(new FileInputStream(new File(
				fFixedMap))).readObject();
	}

	private void printLatencyMetrics(TableWriter writer, Experiment experiment,
			IndexedNeighborGraph graph, int source, int[] ids,
			MetricsCollector result, int[] fixed) {
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
			writer.set("exps", fRepeats);
			writer.emmitRow();

		} else {
			for (int i = 0; i < graph.size(); i++) {
				writer.set("id", experiment.root);
				writer.set("source", source);
				writer.set("target", ids[i]);
				writer.set("fixed", MiscUtils.contains(fixed, i));
				writer.set("edsum", ed.getMetric(i));
				writer.set("rdsum", rd.getMetric(i));
				writer.set("size", graph.size());
				writer.set("exps", fRepeats);
				writer.emmitRow();
			}
		}
	}

	private void printCloudMetrics(TableWriter cloudStats, Experiment exp,
			int source, int[] ids, MetricsCollector result) {

		INodeMetric<Double> total = result.getMetric(SimpleCloudImpl.TOTAL);
		INodeMetric<Double> productive = result
				.getMetric(SimpleCloudImpl.PRODUCTIVE);

		for (int i = 0; i < ids.length; i++) {
			cloudStats.set("id", exp.root);
			cloudStats.set("source", source);
			cloudStats.set("target", ids[i]);
			cloudStats.set("accnup",
					(int) (total.getMetric(i) - productive.getMetric(i)));
			cloudStats.set("accup", productive.getMetric(i).intValue());
			cloudStats.set("exps", fRepeats);
			cloudStats.emmitRow();
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
			final int repetitions, TableWriter core, final int[] fixed)
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
								fixed));
					}
				} catch (Exception ex) {
					System.err
							.println("Submission thread finished with exception.");
					ex.printStackTrace();
				}
			}
		});

		submitter.start();

		MetricsCollector collector = new MetricsCollector();

		if (fNUPAnchor < 0) {
			collector.addAccumulator(new SumAccumulation("ed", graph.size()));
			collector.addAccumulator(new SumAccumulation("rd", graph.size()));
		}

		if (fCloudAssisted) {
			collector.addAccumulator(new SumAccumulation(SimpleCloudImpl.TOTAL,
					graph.size()));
			collector.addAccumulator(new SumAccumulation(
					SimpleCloudImpl.PRODUCTIVE, graph.size()));
		}

		for (int i = 0; i < repetitions; i++) {
			Object value = fExecutor.consume();
			if (isThrowable(value)) {
				printProps(value);
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

	private void printProps(Object value) {
		if (!(value instanceof SimulationTaskException)) {
			return;
		}

		SimulationTaskException exception = (SimulationTaskException) value;
		exception.dumpProperties(System.err);
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
			IndexedNeighborGraph graph, Long seed, int[] fixed)
			throws Exception {

		Pair<EDSimulationEngine, List<INodeMetric<? extends Object>>> elements;
		HashMap<String, Object> props = new HashMap<String, Object>();

		// Seed handling.
		long diffSeed = nextSeed();
		if (seed == null) {
			seed = nextSeed();
		}

		props.put("seed.diffusion", diffSeed);
		props.put("seed.churn", seed);
		props.put("source", source);
		props.put("root", experiment.root);

		Random diffusion = new Random(diffSeed);
		Random churn = new Random(seed);

		// Static experiment.
		if (experiment.lis == null) {
			StaticSimulationBuilder builder = new StaticSimulationBuilder();
			elements = builder.build(fBurnin, fPeriod, experiment, source,
					fSelector, graph, diffusion);
		} else {
			// Create regular processes.
			IProcess[] processes = fYaoChurn.createProcesses(experiment.lis,
					experiment.dis, graph.size(), churn);

			// Then replace fixed ones.
			for (int i = 0; i < fixed.length; i++) {
				processes[fixed[i]] = new FixedProcess(fixed[i], State.up);
			}

			if (fCloudAssisted) {
				elements = new CloudSimulationBuilder(fBurnin, fDelay,
						fNUPBurnin, fSelector.charAt(0), graph, diffusion,
						fNUPAnchor).build(source, processes);
			} else {
				elements = new ChurnSimulationBuilder().build(fBurnin, fPeriod,
						experiment, source, fSelector, graph, diffusion,
						processes);
			}
		}

		return new SimulationTask(
				null,
				elements.a,
				props,
				new Pair[] { new Pair<Integer, List<INodeMetric<? extends Object>>>(
						source, elements.b) });
	}

	private long nextSeed() {
		return System.nanoTime() + fSeedUniquefier++;
	}
}
