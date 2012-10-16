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
import it.unitn.disi.churn.diffusion.experiments.config.ChurnSimulationBuilder;
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
import it.unitn.disi.simulator.measure.SumAccumulation;
import it.unitn.disi.simulator.protocol.FixedProcess;
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.collections.Pair;
import it.unitn.disi.utils.streams.PrefixedWriter;
import it.unitn.disi.utils.tabular.TableWriter;

@AutoConfig
public class DiffusionExperiment2 extends Worker {
	
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
	
	@Attribute(value = "access_delay")
	private double fDelay;

	@Attribute(value = "messages", defaultValue = "1")
	private int fMessages;
	
	@Attribute(value = "cloudassisted", defaultValue = "false")
	private boolean fCloudAssisted;

	@Attribute(value = "fast", defaultValue = "false")
	private boolean f;

	private volatile int fSeedUniquefier;

	private final YaoChurnConfigurator fYaoChurn;

	private final ExperimentReader fReader;

	private IGraphProvider fProvider;

//	private TableWriter fCoreWriter;

	private TableWriter fLatencyWriter;

	private TableWriter fCloudStatWriter;

	private BitSet fFixedMap;

	public DiffusionExperiment2(@Attribute(Attribute.AUTO) IResolver resolver,
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
				"id", "source", "target", "edsum", "rdsum", "size", "fixed",
				"exps", "msgs");

//		fCoreWriter = new TableWriter(new PrefixedWriter("CO:", System.out),
//				"id", "source", "edsum", "size");

		fCloudStatWriter = new TableWriter(
				new PrefixedWriter("CS:", System.out), "id", "source",
				"target", "accup", "accnup", "exps");

		System.err.println("-- Simulation seeds are "
				+ (fFixSeed ? "fixed" : "variable") + ".");

		fFixedMap = fixedNodeMap();
		System.err.print("There are " + fFixedMap.cardinality()
				+ " fixed nodes.");
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
	protected Object load(Integer row) throws Exception {
		Experiment experiment = fReader.readExperiment(row, fProvider);
		IndexedNeighborGraph graph = fProvider.subgraph(experiment.root);
		Random churnSeeds = fFixSeed ? new Random(
				Long.parseLong(experiment.attributes.get("seed"))) : null;

		int source = Integer.parseInt(experiment.attributes.get("node"));
		int[] ids = fProvider.verticesOf(experiment.root);

		return new ExperimentData(experiment, graph, source, ids, cloudNodes(
				experiment, ids, fFixedMap), churnSeeds);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected SimulationTask createTask(int id, Object data) {
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
			elements = builder.build(fBurnin, fPeriod, exp.experiment, source,
					fSelector, graph, diffusion);
		} else {
			// Create regular processes.
			IProcess[] processes = fYaoChurn
					.createProcesses(exp.experiment.lis, exp.experiment.dis,
							graph.size(), churn);

			// Then replace fixed ones.
			for (int i = 0; i < exp.fixed.length; i++) {
				processes[exp.fixed[i]] = new FixedProcess(exp.fixed[i],
						State.up);
			}

			if (fCloudAssisted) {
				elements = new CloudSimulationBuilder(fBurnin, fDelay,
						fNUPBurnin, fSelector.charAt(0), graph, diffusion,
						fNUPAnchor).build(source, fMessages, processes);
			} else {
				elements = new ChurnSimulationBuilder().build(fBurnin, fPeriod,
						exp.experiment, source, fSelector, graph, diffusion,
						processes);
			}
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

		return new Pair<ExperimentData, MetricsCollector>(exp, collector);
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

		if (fNUPAnchor < 0) {
			printLatencies(result.a, result.b);
		}

		if (fCloudAssisted) {
			printCloudAcessStatistics(result.a, result.b);
		}
	}

	private void printCloudAcessStatistics(ExperimentData data,
			MetricsCollector metrics) {

	}

	@SuppressWarnings("unchecked")
	private void printLatencies(ExperimentData data, MetricsCollector metrics) {

		INodeMetric<Double> ed = metrics.getMetric("ed");
		INodeMetric<Double> rd = metrics.getMetric("rd");

		for (int i = 0; i < data.graph.size(); i++) {
			fLatencyWriter.set("id", data.experiment.root);
			fLatencyWriter.set("source", data.source);
			fLatencyWriter.set("target", data.ids[i]);
			fLatencyWriter.set("edsum", ed.getMetric(i));
			fLatencyWriter.set("rdsum", rd.getMetric(i));
			fLatencyWriter.set("size", data.graph.size());
			fLatencyWriter.set("fixed", fFixedMap.get(data.ids[i]));
			fLatencyWriter.set("exps", fRepeat);
			fLatencyWriter.set("msgs", fMessages);

			fLatencyWriter.emmitRow();
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
