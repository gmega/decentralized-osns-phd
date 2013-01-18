package it.unitn.disi.churn.diffusion.experiments;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.IResolver;
import peersim.config.ObjectCreator;

import gnu.trove.list.array.TIntArrayList;
import it.unitn.disi.churn.config.ExperimentReader;
import it.unitn.disi.churn.config.GraphConfigurator;
import it.unitn.disi.churn.config.ExperimentReader.Experiment;
import it.unitn.disi.churn.diffusion.cloud.SimpleCloudImpl;
import it.unitn.disi.churn.diffusion.cloud.ICloud.AccessType;
import it.unitn.disi.churn.diffusion.experiments.config.CloudSimulationBuilder;
import it.unitn.disi.churn.diffusion.experiments.config.StaticSimulationBuilder;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.large.catalog.IGraphProvider;
import it.unitn.disi.simulator.churnmodel.yao.YaoChurnConfigurator;
import it.unitn.disi.simulator.concurrent.SimulationTask;
import it.unitn.disi.simulator.concurrent.Worker;
import it.unitn.disi.simulator.core.EDSimulationEngine;
import it.unitn.disi.simulator.core.IProcess;
import it.unitn.disi.simulator.core.IProcess.State;
import it.unitn.disi.simulator.measure.INodeMetric;
import it.unitn.disi.simulator.measure.MetricsCollector;
import it.unitn.disi.simulator.measure.AvgAccumulation;
import it.unitn.disi.simulator.measure.SumAccumulation;
import it.unitn.disi.simulator.protocol.FixedProcess;
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.collections.Pair;
import it.unitn.disi.utils.streams.PrefixedWriter;
import it.unitn.disi.utils.tabular.TableWriter;

@AutoConfig
public class DiffusionExperimentWorker extends Worker {

	@Attribute("period")
	private double fPeriod;

	@Attribute("selector")
	private String fSelector;

	@Attribute(value = "fixedseeds", defaultValue = "false")
	private boolean fFixSeed;

	@Attribute(value = "nup_anchor", defaultValue = "-1")
	private double fNUPAnchor;

	@Attribute(value = "nup_burnin", defaultValue = "-1")
	private double fNUPBurnin;

	@Attribute(value = "fixed_node_map", defaultValue = "none")
	private String fFixedMapFile;

	@Attribute(value = "p2psims", defaultValue = "true")
	private boolean fP2PSims;

	@Attribute(value = "baseline", defaultValue = "false")
	private boolean fBaseline;

	@Attribute(value = "access_delay")
	private double fDelay;

	@Attribute(value = "fixed_fraction", defaultValue = "0.0")
	private double fFixedFraction;

	@Attribute(value = "messages", defaultValue = "1")
	private int fMessages;

	@Attribute(value = "quench_desync", defaultValue = "1")
	private int fQuenchDesync;

	@Attribute(value = "cloudassisted", defaultValue = "false")
	private boolean fCloudAssisted;

	@Attribute(value = "fast", defaultValue = "false")
	private boolean f;

	@Attribute(value = "randomized", defaultValue = "false")
	private boolean fRandomized;

	@Attribute(value = "login_grace", defaultValue = "0.00138888888")
	private double fLoginGrace;

	private volatile int fSeedUniquefier;

	private final YaoChurnConfigurator fYaoChurn;

	private final ExperimentReader fReader;

	private IGraphProvider fProvider;

	private TableWriter fLatencyWriter;

	private TableWriter fP2PCostWriter;

	private TableWriter fBaselineLatencyWriter;

	private TableWriter fCloudStatWriter;

	private TableWriter fBaselineCStatWriter;

	private BitSet fFixedMap;

	public DiffusionExperimentWorker(
			@Attribute(Attribute.AUTO) IResolver resolver,
			@Attribute(value = "churn", defaultValue = "false") boolean churn)
			throws Exception {
		super(resolver);

		fProvider = ObjectCreator.createInstance(GraphConfigurator.class, "",
				resolver).graphProvider();

		fYaoChurn = churn ? ObjectCreator.createInstance(
				YaoChurnConfigurator.class, "", resolver) : null;

		fReader = new ExperimentReader("id");
		ObjectCreator
				.fieldInject(ExperimentReader.class, fReader, "", resolver);
	}

	protected void initialize() throws Exception {
		fLatencyWriter = new TableWriter(new PrefixedWriter("ES:", System.out),
				"id", "source", "target", "edsumd", "edsum", "edsumu",
				"rdsumd", "rdsum", "rdsumu", "size", "fixed", "exps", "msgs");

		fP2PCostWriter = new TableWriter(new PrefixedWriter("PC:", System.out),
				"id", "source", "target", "uprec", "nuprec", "msgtime");

		fBaselineLatencyWriter = new TableWriter(new PrefixedWriter("ESB:",
				System.out), "id", "source", "target", "b.edsumd", "b.edsum",
				"b.edsumu", "b.rdsumd", "b.rdsum", "b.rdsumu", "size", "fixed",
				"exps", "msgs");

		fCloudStatWriter = new TableWriter(
				new PrefixedWriter("CS:", System.out), "id", "source",
				"target", "totup", "totnup", "totime", "updup", "updnup",
				"updtime");

		fBaselineCStatWriter = new TableWriter(new PrefixedWriter("CSB:",
				System.out), "id", "source", "target", "b.totup", "b.totnup",
				"b.totime", "b.updup", "b.updnup", "b.updtime");

		System.err.println("-- Simulation seeds are "
				+ (fFixSeed ? "fixed" : "variable") + ".");
		fFixedMap = fixedNodeMap();
		System.err.println("There are " + fFixedMap.cardinality()
				+ " fixed nodes.");

		if (fCloudAssisted) {
			System.err.println("-- Cloud sims are on.");
			System.err.println("-- Quench runs every " + fQuenchDesync
					+ " rounds.");
			if (fBaseline) {
				System.err.println("-- Baseline cloud sims are on.");
			}

			if (fRandomized) {
				System.err.println("-- Periods are randomized  ["
						+ (fDelay * fFixedFraction) + "/"
						+ (fDelay * (1.0 - fFixedFraction)) + "]");
			} else {
				System.err.println("-- Periods are fixed:" + fDelay);
			}
		}

	}

	private BitSet fixedNodeMap() throws Exception {
		if (fFixedMap == null) {
			return new BitSet();
		}

		ObjectInputStream stream = null;
		try {
			stream = new ObjectInputStream(new FileInputStream(new File(
					fFixedMapFile)));
			return (BitSet) stream.readObject();
		} finally {
			stream.close();
		}
	}

	@Override
	protected Serializable load(Integer row) throws Exception {
		Experiment experiment = fReader.readExperiment(row, fProvider);
		IndexedNeighborGraph graph = fProvider.subgraph(experiment.root);
		Random churnSeeds = fFixSeed ? new Random(
				Long.parseLong(experiment.attributes.get("seed"))) : null;

		int source = Integer.parseInt(experiment.attributes.get("node"));
		int[] ids = fProvider.verticesOf(experiment.root);

		for (int i = 0; i < ids.length; i++) {
			System.out.println("ID:" + ids[i] + " " + i);
		}

		return new ExperimentData(experiment, graph, source, ids, cloudNodes(
				experiment, ids, fFixedMap), churnSeeds);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected SimulationTask createTask(int id, Serializable data) {
		ExperimentData exp = (ExperimentData) data;
		IndexedNeighborGraph graph = exp.graph;

		Pair<EDSimulationEngine, List<INodeMetric<? extends Object>>> elements;
		HashMap<String, Object> props = new HashMap<String, Object>();

		// Seed handling.
		long diffSeed = nextSeed();
		long churnSeed = exp.churnSeeds == null ? nextSeed() : exp.churnSeeds
				.nextLong();

		props.put("seed.diffusion", diffSeed);
		props.put("seed.churn", churnSeed);
		props.put("source", exp.source);
		props.put("root", exp.experiment.root);

		Random diffusion = new Random(diffSeed);
		Random churn = new Random(churnSeed);

		int source = MiscUtils.indexOf(exp.ids, exp.source);

		// Static experiment.
		if (exp.experiment.lis == null) {
			StaticSimulationBuilder builder = new StaticSimulationBuilder();
			elements = builder.build(fPeriod, exp.experiment, source,
					fSelector, graph, diffusion);
		}

		// Experiments with churn.
		else {
			// Create regular processes.
			IProcess[] processes = fYaoChurn
					.createProcesses(exp.experiment.lis, exp.experiment.dis,
							graph.size(), churn);

			// Then replace fixed ones.
			for (int i = 0; i < exp.fixed.length; i++) {
				processes[exp.fixed[i]] = new FixedProcess(exp.fixed[i],
						State.up);
			}

			elements = new CloudSimulationBuilder(fBurnin, fDelay, fNUPBurnin,
					fSelector.charAt(0), graph, diffusion, fNUPAnchor,
					fLoginGrace, fFixedFraction, fRandomized).build(source,
					fMessages, fQuenchDesync, fP2PSims, fCloudAssisted,
					fBaseline, processes);
		}

		return new SimulationTask(
				data,
				elements.a,
				props,
				new Pair[] { new Pair<Integer, List<INodeMetric<? extends Object>>>(
						exp.source, elements.b) });
	}

	@Override
	protected Serializable resultAggregate(int id, Object data) {
		ExperimentData exp = (ExperimentData) data;
		IndexedNeighborGraph graph = exp.graph;
		MetricsCollector collector = new MetricsCollector();

		addCloudMetrics("", graph, collector);
		if (fBaseline) {
			addCloudMetrics("b", graph, collector);
		}

		if (fCloudAssisted) {
			collector.addAccumulator(new AvgAccumulation(SimpleCloudImpl.TOTAL,
					graph.size()));
			collector.addAccumulator(new AvgAccumulation(
					SimpleCloudImpl.PRODUCTIVE, graph.size()));
		}

		return new Pair<ExperimentData, MetricsCollector>(exp, collector);
	}

	public void addCloudMetrics(String prefix, IndexedNeighborGraph graph,
			MetricsCollector collector) {

		// These have to be divided by the number of messages to yield the
		// real average.
		collector.addAccumulator(new AvgAccumulation(prefix(prefix, "ed"),
				graph.size()));
		collector.addAccumulator(new AvgAccumulation(prefix(prefix, "rd"),
				graph.size()));

		// These have also to be divided by the number of experiments.
		collector.addAccumulator(new SumAccumulation(prefix(prefix,
				"cloud_all." + AccessType.nup), graph.size()));
		collector.addAccumulator(new SumAccumulation(prefix(prefix,
				"cloud_all." + AccessType.productive), graph.size()));
		collector.addAccumulator(new SumAccumulation(prefix(prefix,
				"cloud_all.accrued"), 1));

		collector.addAccumulator(new SumAccumulation(prefix(prefix,
				"cloud_upd." + AccessType.nup), graph.size()));
		collector.addAccumulator(new SumAccumulation(prefix(prefix,
				"cloud_upd." + AccessType.productive), graph.size()));
		collector.addAccumulator(new SumAccumulation(prefix(prefix,
				"cloud_upd.accrued"), 1));

		collector.addAccumulator(new SumAccumulation("msg.up", graph.size()));
		collector.addAccumulator(new SumAccumulation("msg.nup", graph.size()));
		collector.addAccumulator(new SumAccumulation("msg.accrued", 1));
	}

	private String prefix(String prefix, String string) {
		if (prefix.equals("")) {
			return string;
		}
		return prefix + "." + string;
	}

	@Override
	protected void aggregate(Object aggregate, int i, SimulationTask task) {
		ExperimentData exp = (ExperimentData) task.id();
		List<? extends INodeMetric<?>> metrics = task.metric(exp.source);
		@SuppressWarnings("unchecked")
		Pair<ExperimentData, MetricsCollector> pair = (Pair<ExperimentData, MetricsCollector>) aggregate;
		pair.b.add(metrics);
	}

	@Override
	protected void outputResults(Object resultObj) {
		@SuppressWarnings("unchecked")
		Pair<ExperimentData, MetricsCollector> result = (Pair<ExperimentData, MetricsCollector>) resultObj;

		printLatencies("", fLatencyWriter, result.a, result.b);
		printP2PCosts(fP2PCostWriter, result.a, result.b);

		if (fCloudAssisted) {
			printCloudAcessStatistics("", fCloudStatWriter, result.a, result.b);
			if (fBaseline) {
				printLatencies("b", fBaselineLatencyWriter, result.a, result.b);
				printCloudAcessStatistics("b", fBaselineCStatWriter, result.a,
						result.b);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void printP2PCosts(TableWriter writer, ExperimentData data,
			MetricsCollector metrics) {
		INodeMetric<Double> updates = metrics.getMetric("msg.up");
		INodeMetric<Double> noUpdates = metrics.getMetric("msg.nup");
		INodeMetric<Double> time = metrics.getMetric("msg.accrued");

		for (int i = 0; i < data.graph.size(); i++) {
			writer.set("id", data.experiment.root);
			writer.set("source", data.source);
			writer.set("target", data.ids[i]);

			writer.set("uprec", updates.getMetric(i));
			writer.set("nuprec", noUpdates.getMetric(i));
			writer.set("msgtime", time.getMetric(0));

			writer.emmitRow();
		}
	}

	@SuppressWarnings("unchecked")
	private void printCloudAcessStatistics(String prefix, TableWriter writer,
			ExperimentData data, MetricsCollector metrics) {

		INodeMetric<Double> totup = metrics.getMetric(prefix(prefix,
				"cloud_all." + AccessType.productive));
		INodeMetric<Double> totnup = metrics.getMetric(prefix(prefix,
				"cloud_all." + AccessType.nup));
		INodeMetric<Double> totime = metrics.getMetric(prefix(prefix,
				"cloud_all.accrued"));

		INodeMetric<Double> updup = metrics.getMetric(prefix(prefix,
				"cloud_upd." + AccessType.productive));
		INodeMetric<Double> updnup = metrics.getMetric(prefix(prefix,
				"cloud_upd." + AccessType.nup));
		INodeMetric<Double> updtime = metrics.getMetric(prefix(prefix,
				"cloud_upd.accrued"));

		for (int i = 0; i < data.graph.size(); i++) {
			writer.set("id", data.experiment.root);
			writer.set("source", data.source);
			writer.set("target", data.ids[i]);

			writer.set(prefix(prefix, "totup"), totup.getMetric(i));
			writer.set(prefix(prefix, "totnup"), totnup.getMetric(i));
			writer.set(prefix(prefix, "totime"), totime.getMetric(0));

			writer.set(prefix(prefix, "updup"), updup.getMetric(i));
			writer.set(prefix(prefix, "updnup"), updnup.getMetric(i));
			writer.set(prefix(prefix, "updtime"), updtime.getMetric(0));

			writer.emmitRow();
		}
	}

	private void printLatencies(String prefix, TableWriter writer,
			ExperimentData data, MetricsCollector metrics) {

		AvgAccumulation ed = (AvgAccumulation) metrics.getMetric(prefix(prefix,
				"ed"));
		AvgAccumulation rd = (AvgAccumulation) metrics.getMetric(prefix(prefix,
				"rd"));

		for (int i = 0; i < data.graph.size(); i++) {
			writer.set("id", data.experiment.root);
			writer.set("source", data.source);
			writer.set("target", data.ids[i]);
			writer.set(prefix(prefix, "edsumd"), ed.lowerConfidenceLimit(i));
			writer.set(prefix(prefix, "edsum"), ed.getMetric(i));
			writer.set(prefix(prefix, "edsumu"), ed.upperConfidenceLimit(i));
			writer.set(prefix(prefix, "rdsumd"), rd.lowerConfidenceLimit(i));
			writer.set(prefix(prefix, "rdsum"), rd.getMetric(i));
			writer.set(prefix(prefix, "rdsumu"), rd.upperConfidenceLimit(i));
			writer.set("size", data.graph.size());
			writer.set("fixed", fFixedMap.get(data.ids[i]));
			writer.set("exps", fRepeat);
			writer.set("msgs", fMessages);

			writer.emmitRow();
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

	private long nextSeed() {
		return System.nanoTime() + fSeedUniquefier++;
	}

	private static class ExperimentData implements Serializable {

		private static final long serialVersionUID = 1L;

		private final Experiment experiment;
		private final IndexedNeighborGraph graph;
		private final int[] ids;
		private final int[] fixed;
		private final int source;
		private final Random churnSeeds;

		public ExperimentData(Experiment experiment,
				IndexedNeighborGraph graph, int source, int[] ids, int[] fixed,
				Random churnSeeds) {
			this.source = source;
			this.experiment = experiment;
			this.graph = graph;
			this.ids = ids;
			this.fixed = fixed;
			this.churnSeeds = churnSeeds;
		}

		public String toString() {
			return "root, source, size = (" + this.experiment.root + ", "
					+ this.source + ", " + graph.size() + ")";
		}
	}

	@Override
	protected boolean isPreciseEnough(Object aggregate) {
		return false;
	}

}
