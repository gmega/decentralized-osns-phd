package it.unitn.disi.churn.diffusion;

import it.unitn.disi.churn.config.YaoChurnConfigurator;
import it.unitn.disi.churn.config.ExperimentReader.Experiment;
import it.unitn.disi.churn.diffusion.churn.CachingTransformer;
import it.unitn.disi.churn.diffusion.churn.LiveTransformer;
import it.unitn.disi.churn.simulator.CyclicProtocolRunner;
import it.unitn.disi.churn.simulator.CyclicSchedulable;
import it.unitn.disi.churn.simulator.FixedProcess;
import it.unitn.disi.churn.simulator.IEventObserver;
import it.unitn.disi.churn.simulator.IProcess;
import it.unitn.disi.churn.simulator.RenewalProcess;
import it.unitn.disi.churn.simulator.SimpleEDSim;
import it.unitn.disi.churn.simulator.IProcess.State;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.network.churn.yao.YaoInit.IDistributionGenerator;
import it.unitn.disi.utils.collections.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;

public class SimulationTask implements Callable<double[]> {

	private final double fBurnin;

	private final double fPeriod;

	private final Experiment fExperiment;

	private final int fSource;

	private final IndexedNeighborGraph fGraph;

	private final Random fRandom;

	private final YaoChurnConfigurator fYaoConf;

	private final String fPeerSelector;

	public SimulationTask(double burnin, double period, Experiment experiment,
			YaoChurnConfigurator yaoConf, int source, String peerSelector,
			IndexedNeighborGraph graph, Random random) {
		fBurnin = burnin;
		fPeriod = period;
		fExperiment = experiment;
		fSource = source;
		fGraph = graph;
		fRandom = random;
		fYaoConf = yaoConf;
		fPeerSelector = peerSelector;
	}

	@Override
	public double[] call() throws Exception {

		CyclicProtocolRunner<HistoryForwarding> ps = protocols(fGraph, fRandom);
		IProcess[] processes = processes(fExperiment, fSource, fGraph, fRandom);

		List<Pair<Integer, ? extends IEventObserver>> observers = new ArrayList<Pair<Integer, ? extends IEventObserver>>();
		observers.add(new Pair<Integer, IEventObserver>(
				IProcess.PROCESS_SCHEDULABLE_TYPE, ps.get(fSource)
						.sourceEventObserver()));
		observers.add(new Pair<Integer, IEventObserver>(1, ps));

		SimpleEDSim bcs = new SimpleEDSim(processes, observers, fBurnin);
		bcs.schedule(new CyclicSchedulable(fPeriod, 1));
		bcs.run();

		double base = ((HistoryForwarding) ps.get(fSource)).latency();

		double [] latencies = new double[fGraph.size()];
		for (int i = 0; i < processes.length; i++) {
			latencies[i] = (((HistoryForwarding) ps.get(i)).latency() - base);
		}
		
		return latencies;
	}

	private IProcess[] processes(Experiment experiment, int source,
			IndexedNeighborGraph graph, Random r) {

		IProcess[] rp = new IProcess[graph.size()];

		for (int i = 0; i < rp.length; i++) {
			if (staticExperiment(experiment)) {
				rp[i] = new FixedProcess(i, State.up);
			} else {
				IDistributionGenerator dgen = fYaoConf.distributionGenerator();
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
		switch (fPeerSelector.charAt(0)) {
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
