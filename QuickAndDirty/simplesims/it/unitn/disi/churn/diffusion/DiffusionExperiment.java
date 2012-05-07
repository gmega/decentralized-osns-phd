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
import peersim.util.IncrementalStats;

import it.unitn.disi.churn.config.GraphConfigurator;
import it.unitn.disi.churn.simulator.FixedProcess;
import it.unitn.disi.churn.simulator.IEventObserver;
import it.unitn.disi.churn.simulator.IProcess;
import it.unitn.disi.churn.simulator.IProcess.State;
import it.unitn.disi.churn.simulator.PeriodicSchedulable;
import it.unitn.disi.churn.simulator.SimpleEDSim;
import it.unitn.disi.churn.simulator.CyclicProtocolRunner;
import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.large.catalog.IGraphProvider;
import it.unitn.disi.newscasting.experiments.schedulers.IScheduleIterator;
import it.unitn.disi.newscasting.experiments.schedulers.SchedulerFactory;
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

	private IResolver fResolver;

	public DiffusionExperiment(@Attribute(Attribute.AUTO) IResolver resolver) {
		fConfig = ObjectCreator.createInstance(GraphConfigurator.class, "",
				resolver);
		fResolver = resolver;
	}

	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {
		IGraphProvider provider = fConfig.graphProvider();
		IScheduleIterator it = SchedulerFactory.getInstance()
				.createScheduler(fResolver, "").iterator();

		TableWriter writer = new TableWriter(new PrefixedWriter("ES:", oup),
				"id", "lsum", "lsqrsum", "n");

		IncrementalStats stats = new IncrementalStats();

		Integer id;
		while ((id = (Integer) it.nextIfAvailable()) != IScheduleIterator.DONE) {
			IndexedNeighborGraph graph = provider.subgraph(id);
			int[] ids = provider.verticesOf(id);
			double[] latencies = new double[ids.length];

			stats.reset();
			runExperiment(graph, latencies);

			for (int i = 0; i < latencies.length; i++) {
				stats.add(latencies[i]);
			}

			writer.set("id", id);
			writer.set("lsum", stats.getSum());
			writer.set("lsqrsum", stats.getSqrSum());
			writer.set("n", stats.getN());
			writer.emmitRow();
		}
	}

	private void runExperiment(IndexedNeighborGraph graph, double[] latencies)
			throws Exception {
		Random r = new Random();

		CyclicProtocolRunner ps = protocols(graph, r);
		IProcess[] processes = processes(graph, r);

		List<Pair<Integer, ? extends IEventObserver>> observers = new ArrayList<Pair<Integer, ? extends IEventObserver>>();
		observers.add(new Pair<Integer, IEventObserver>(1, ps));

		SimpleEDSim bcs = new SimpleEDSim(processes, observers, fBurnin);
		bcs.schedule(new PeriodicSchedulable(fPeriod, 1));
		bcs.run();

		for (int i = 0; i < processes.length; i++) {
			latencies[i] += ((HistoryForwarding) ps.get(i)).latency();
		}
	}

	private IProcess[] processes(IndexedNeighborGraph graph, Random r) {
		IProcess[] rp = new IProcess[graph.size()];
		for (int i = 0; i < rp.length; i++) {
			rp[i] = new FixedProcess(i, State.up);
		}

		return rp;
	}

	private CyclicProtocolRunner protocols(IndexedNeighborGraph graph, Random r) {
		HistoryForwarding[] protos = new HistoryForwarding[graph.size()];
		for (int i = 0; i < graph.size(); i++) {
			protos[i] = new HistoryForwarding(graph, peerSelector(r), i);
		}

		// Dissemination from node at the center.
		protos[0].reached(0);

		CyclicProtocolRunner ps = new CyclicProtocolRunner(protos);
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
