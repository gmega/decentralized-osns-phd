package it.unitn.disi.churn.diffusion;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.IResolver;
import peersim.config.ObjectCreator;

import it.unitn.disi.churn.config.ExperimentReader;
import it.unitn.disi.churn.config.ExperimentReader.Experiment;
import it.unitn.disi.churn.config.GraphConfigurator;
import it.unitn.disi.churn.config.YaoChurnConfigurator;
import it.unitn.disi.churn.diffusion.churn.CachingTransformer;
import it.unitn.disi.churn.diffusion.churn.LiveTransformer;
import it.unitn.disi.churn.simulator.FixedProcess;
import it.unitn.disi.churn.simulator.IEventObserver;
import it.unitn.disi.churn.simulator.IProcess;
import it.unitn.disi.churn.simulator.RenewalProcess;
import it.unitn.disi.churn.simulator.IProcess.State;
import it.unitn.disi.churn.simulator.CyclicSchedulable;
import it.unitn.disi.churn.simulator.SimpleEDSim;
import it.unitn.disi.churn.simulator.CyclicProtocolRunner;
import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.large.catalog.IGraphProvider;
import it.unitn.disi.network.churn.yao.YaoInit.IDistributionGenerator;
import it.unitn.disi.newscasting.experiments.schedulers.IScheduleIterator;
import it.unitn.disi.newscasting.experiments.schedulers.SchedulerFactory;
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.collections.Pair;
import it.unitn.disi.utils.streams.PrefixedWriter;
import it.unitn.disi.utils.tabular.TableWriter;

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

	private YaoChurnConfigurator fYaoChurn;

	private IResolver fResolver;

	private ExperimentReader fReader;

	public DiffusionExperiment(@Attribute(Attribute.AUTO) IResolver resolver,
			@Attribute(value = "churn", defaultValue = "false") boolean churn) {
		fConfig = ObjectCreator.createInstance(GraphConfigurator.class, "",
				resolver);

		if (churn) {
			fYaoChurn = ObjectCreator.createInstance(
					YaoChurnConfigurator.class, "", resolver);
		}
		
		fReader = new ExperimentReader("id");
		ObjectCreator.fieldInject(ExperimentReader.class, fReader, "",
				resolver);

		fResolver = resolver;
	}

	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {
		IGraphProvider provider = fConfig.graphProvider();
		IScheduleIterator it = SchedulerFactory.getInstance()
				.createScheduler(fResolver, "").iterator();

		TableWriter writer = new TableWriter(new PrefixedWriter("ES:", oup),
				"id", "source", "target", "lsum");

		Integer row;
		while ((row = (Integer) it.nextIfAvailable()) != IScheduleIterator.DONE) {

			Experiment experiment = fReader.readExperiment(row, provider);

			IndexedNeighborGraph graph = provider.subgraph(experiment.root);
			int source = Integer.parseInt(experiment.attributes.get("node"));

			int[] ids = provider.verticesOf(experiment.root);
			double[] latencies = new double[ids.length];

			runExperiments(experiment, MiscUtils.indexOf(ids, source), graph,
					latencies, fRepeats);

			for (int i = 0; i < latencies.length; i++) {
				writer.set("id", experiment.root);
				writer.set("source", source);
				writer.set("target", ids[i]);
				writer.set("lsum", latencies[i]);
				writer.emmitRow();
			}

		}
	}

	private void runExperiments(Experiment experiment, int source,
			IndexedNeighborGraph graph, double[] latencies, int repetitions)
			throws Exception {
		for (int i = 0; i < repetitions; i++) {
			runExperiment(experiment, source, graph, latencies);
		}
	}

	private void runExperiment(Experiment experiment, int source,
			IndexedNeighborGraph graph, double[] latencies) throws Exception {
		Random r = new Random();

		CyclicProtocolRunner<HistoryForwarding> ps = protocols(graph, r);
		IProcess[] processes = processes(experiment, source, graph, r);

		List<Pair<Integer, ? extends IEventObserver>> observers = new ArrayList<Pair<Integer, ? extends IEventObserver>>();
		observers.add(new Pair<Integer, IEventObserver>(
				IProcess.PROCESS_SCHEDULABLE_TYPE, ps.get(source)
						.sourceEventObserver()));
		observers.add(new Pair<Integer, IEventObserver>(1, ps));

		SimpleEDSim bcs = new SimpleEDSim(processes, observers, fBurnin);
		bcs.schedule(new CyclicSchedulable(fPeriod, 1));
		bcs.run();

		double base = ((HistoryForwarding) ps.get(source)).latency();

		for (int i = 0; i < processes.length; i++) {
			latencies[i] += (((HistoryForwarding) ps.get(i)).latency() - base);
		}
	}

	private IProcess[] processes(Experiment experiment, int source,
			IndexedNeighborGraph graph, Random r) {

		IProcess[] rp = new IProcess[graph.size()];

		for (int i = 0; i < rp.length; i++) {
			if (staticExperiment(experiment)) {
				rp[i] = new FixedProcess(i, State.up);
			} else {
				IDistributionGenerator dgen = fYaoChurn.distributionGenerator();
				rp[i] = new RenewalProcess(i,
						dgen.uptimeDistribution(experiment.lis[i]),
						dgen.downtimeDistribution(experiment.dis[i]),
						State.down);
			}
		}
		return rp;
	}

	private boolean staticExperiment(Experiment experiment) {
		return experiment.lis == null;
	}

	private CyclicProtocolRunner<HistoryForwarding> protocols(
			IndexedNeighborGraph graph, Random r) {
		HistoryForwarding[] protos = new HistoryForwarding[graph.size()];
		CachingTransformer caching = new CachingTransformer(
				new LiveTransformer());

		for (int i = 0; i < graph.size(); i++) {
			protos[i] = new HistoryForwarding(graph, peerSelector(r), caching,
					i);
		}

		CyclicProtocolRunner<HistoryForwarding> ps = new CyclicProtocolRunner<HistoryForwarding>(
				protos);
		return ps;
	}

	private IPeerSelector peerSelector(Random r) {
		switch (fSelector.charAt(0)) {
		case 'a':
			return new BiasedCentralitySelector(r, true);
		case 'r':
			return new RandomSelector(r);
		case 'c':
			return new BiasedCentralitySelector(r, false);
		default:
			throw new UnsupportedOperationException();
		}
	}

}
