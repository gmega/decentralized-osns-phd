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
import it.unitn.disi.churn.config.Experiment;
import it.unitn.disi.churn.config.ExperimentReader;
import it.unitn.disi.churn.config.GraphConfigurator;
import it.unitn.disi.churn.diffusion.CoreTracker;
import it.unitn.disi.churn.diffusion.cloud.SimpleCloudImpl;
import it.unitn.disi.churn.diffusion.cloud.ICloud.AccessType;
import it.unitn.disi.churn.diffusion.experiments.config.CloudSimulationBuilder;
import it.unitn.disi.churn.diffusion.experiments.config.StaticSimulationBuilder;
import it.unitn.disi.churn.diffusion.experiments.config.Utils;
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

	// Cycle of push protocol.
	@Attribute("push_cycle_length")
	private double fPeriod;

	// Selector of push protocol.
	@Attribute("push_selector")
	private String fSelector;

	// Timeout (period without contacts before giving up) of push protocol.
	@Attribute(value = "push_timeout")
	private double fPushTimeout;

	// Cycle of antientropy.
	@Attribute(value = "antientropy_cycle_length")
	private double fAECycleLength;

	// "Grace period" for expired PeriodicActions.
	@Attribute(value = "login_grace", defaultValue = "0")
	private double fLoginGrace;

	@Attribute(value = "latency_bound")
	private double fDelta;

	// Fixed fraction of HYBRID's timer.
	@Attribute(value = "fixed_fraction", defaultValue = "0.0")
	private double fFixedFraction;

	// Slowdown factor for push of QUENCH messages respectively to
	// updates.
	@Attribute(value = "quench_desync", defaultValue = "1")
	private int fQuenchDesync;

	// Burn-in period between when NUP messages start to be fired and
	// when we allow the update to be posted.
	@Attribute(value = "nup_burnin", defaultValue = "-1")
	private double fNUPBurnin;

	@Attribute(value = "fixedseeds", defaultValue = "false")
	private boolean fFixSeed;

	@Attribute(value = "nup_anchor", defaultValue = "-1")
	private double fNUPAnchor;

	@Attribute(value = "fixed_node_map", defaultValue = "none")
	private String fFixedMapFile;

	@Attribute(value = "p2psims", defaultValue = "true")
	private boolean fP2PSims;

	@Attribute(value = "baseline", defaultValue = "false")
	private boolean fBaseline;

	@Attribute(value = "messages", defaultValue = "1")
	private int fMessages;

	@Attribute(value = "track.cores", defaultValue = "false")
	private boolean fTrackCores;

	@Attribute(value = "cloudassisted", defaultValue = "false")
	private boolean fCloudAssisted;

	@Attribute(value = "summary", defaultValue = "false")
	private boolean fSummary;

	@Attribute(value = "randomized", defaultValue = "false")
	private boolean fRandomized;

	private volatile int fSeedUniquefier;

	private final YaoChurnConfigurator fYaoChurn;

	private final ExperimentReader fReader;

	private IGraphProvider fProvider;

	private BitSet fFixedMap;

	// -------------------------------------------------------------------------
	// Reflex that writers have an inflexible API.

	private TableWriter fLatencyWriter;

	private TableWriter fP2PCostWriter;

	private TableWriter fBaselineLatencyWriter;

	private TableWriter fCloudStatWriter;

	private TableWriter fBaselineCStatWriter;

	private TableWriter fSummaryWriter;

	private TableWriter fCoreTracker;

	// -------------------------------------------------------------------------

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
				"id", "source", "target", "hfuprec", "hfnuprec", "aeuprec",
				"aenuprec", "aeupsend", "aenupsend", "aeinit", "aerespond",
				"msgtime");

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

		fSummaryWriter = new TableWriter(
				new PrefixedWriter("SUM:", System.out), "id", "rdavg", "rdsum",
				"size");

		fCoreTracker = fTrackCores ? new TableWriter(new PrefixedWriter("COR:",
				System.out), "id", "source", "rdavg", "rdsum", "edavg",
				"edsum", "size", "coresize") : null;

		System.err.println("-- Simulation seeds are "
				+ (fFixSeed ? "fixed" : "variable") + ".");
		fFixedMap = fixedNodeMap();
		System.err.println("There are " + fFixedMap.cardinality()
				+ " fixed nodes.");

		System.err.println("-- Selector is (" + fSelector + ").");
		System.err.println("Push protocol timeout is " + fPushTimeout + ".");
		System.err.println("Antientropy is "
				+ ((fAECycleLength < 0) ? "disabled " : ("enabled (period is "
						+ fAECycleLength + ")")));

		if (fCloudAssisted) {
			System.err.println("-- Cloud sims are on.");
			System.err.println("-- Quench runs every " + fQuenchDesync
					+ " rounds.");
			if (fBaseline) {
				System.err.println("-- Baseline cloud sims are on.");
			}

			if (fRandomized) {
				System.err.println("-- Periods are randomized  ["
						+ (fPeriod * fFixedFraction) + ", " + fPeriod + "]");
			} else {
				System.err.println("-- Periods are fixed:" + fPeriod);
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

		return new ExperimentData(experiment, graph, source, ids,
				fYaoChurn != null ? cloudNodes(experiment, ids, fFixedMap)
						: null, churnSeeds);
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
			elements = builder.build(fPeriod, exp.experiment,
					MiscUtils.indexOf(exp.ids, exp.experiment.root), source,
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

			elements = new CloudSimulationBuilder(fBurnin, fDelta, fNUPBurnin,
					fSelector.charAt(0), graph, diffusion, fNUPAnchor,
					fLoginGrace, fFixedFraction, fRandomized)
					.build(source, fMessages, fQuenchDesync, fPushTimeout,
							fAECycleLength, fP2PSims, fCloudAssisted,
							fBaseline, fTrackCores, processes);
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

		collector.addAccumulator(new SumAccumulation("msg.hflood.up", graph.size()));
		collector.addAccumulator(new SumAccumulation("msg.hflood.nup", graph.size()));
		
		collector.addAccumulator(new SumAccumulation("msg.ae.rec.up", graph.size()));
		collector.addAccumulator(new SumAccumulation("msg.ae.rec.nup", graph.size()));
		
		collector.addAccumulator(new SumAccumulation("msg.ae.sent.up", graph.size()));
		collector.addAccumulator(new SumAccumulation("msg.ae.sent.nup", graph.size()));
		
		collector.addAccumulator(new SumAccumulation("msg.ae.init", graph.size()));
		collector.addAccumulator(new SumAccumulation("msg.ae.respond", graph.size()));
		
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

		// Core tracking is done per sample.
		if (fTrackCores) {
			int size = exp.graph.size();

			CoreTracker tracker = (CoreTracker) Utils.lookup(metrics,
					"coremembership", Boolean.class);

			double edsum = sum(Utils.lookup(metrics, "ed", Double.class), size,
					tracker);
			double rdsum = sum(Utils.lookup(metrics, "rd", Double.class), size,
					tracker);

			fCoreTracker.set("id", exp.experiment.root);
			fCoreTracker.set("source", exp.source);
			fCoreTracker.set("rdavg", rdsum / size);
			fCoreTracker.set("rdsum", rdsum);
			fCoreTracker.set("edavg", edsum / size);
			fCoreTracker.set("edsum", edsum);
			fCoreTracker.set("size", size);
			fCoreTracker.set("coresize", tracker.coreSize());
			fCoreTracker.emmitRow();
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void outputResults(Object resultObj) {
		Pair<ExperimentData, MetricsCollector> result = (Pair<ExperimentData, MetricsCollector>) resultObj;
		if (fSummary) {
			outputSummarized(result);
		} else {
			outputRegular(result);
		}

	}

	private void outputRegular(Pair<ExperimentData, MetricsCollector> result) {
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

	private void outputSummarized(Pair<ExperimentData, MetricsCollector> result) {
		MetricsCollector metrics = result.b;
		AvgAccumulation rd = (AvgAccumulation) metrics.getMetric("rd");

		int size = result.a.graph.size();
		double sum = sum(rd, size);

		fSummaryWriter.set("id", result.a.experiment.root);
		fSummaryWriter.set("rdsum", sum);
		fSummaryWriter.set("size", size);
		fSummaryWriter.set("rdavg", sum / size);

		fSummaryWriter.emmitRow();
	}

	private double sum(INodeMetric<Double> rd, int size) {
		double sum = 0;
		for (int i = 0; i < size; i++) {
			sum += rd.getMetric(i);
		}
		return sum;
	}

	private double sum(INodeMetric<Double> rd, int size, CoreTracker tracker) {
		double sum = 0;
		for (int i = 0; i < size; i++) {
			if (tracker.isPartOfConnectedCore(i)) {
				sum += rd.getMetric(i);
			}
		}
		return sum;
	}

	@SuppressWarnings("unchecked")
	private void printP2PCosts(TableWriter writer, ExperimentData data,
			MetricsCollector metrics) {
		INodeMetric<Double> hfloodUpdates = metrics.getMetric("msg.hflood.up");
		INodeMetric<Double> hfloodQuench = metrics.getMetric("msg.hflood.nup");

		INodeMetric<Double> aeUpdatesRec = metrics.getMetric("msg.ae.rec.up");
		INodeMetric<Double> aeQuenchRec = metrics.getMetric("msg.ae.rec.nup");

		INodeMetric<Double> aeUpdatesSent = metrics.getMetric("msg.ae.sent.up");
		INodeMetric<Double> aeQuenchSent = metrics.getMetric("msg.ae.sent.nup");

		INodeMetric<Double> aeInitiated = metrics.getMetric("msg.ae.init");
		INodeMetric<Double> aeReceived = metrics.getMetric("msg.ae.respond");

		INodeMetric<Double> time = metrics.getMetric("msg.accrued");

		for (int i = 0; i < data.graph.size(); i++) {
			writer.set("id", data.experiment.root);
			writer.set("source", data.source);
			writer.set("target", data.ids[i]);

			writer.set("hfuprec", hfloodUpdates.getMetric(i));
			writer.set("hfnuprec", hfloodQuench.getMetric(i));

			writer.set("aeuprec", aeUpdatesRec.getMetric(i));
			writer.set("aenuprec", aeQuenchRec.getMetric(i));

			writer.set("aeupsend", aeUpdatesSent.getMetric(i));
			writer.set("aenupsend", aeQuenchSent.getMetric(i));

			writer.set("aeinit", aeInitiated.getMetric(i));
			writer.set("aerespond", aeReceived.getMetric(i));

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

	@Override
	protected boolean quiet(int id, Serializable data) {
		// Static simulations are quiet.
		return fYaoChurn == null;
	}

}
