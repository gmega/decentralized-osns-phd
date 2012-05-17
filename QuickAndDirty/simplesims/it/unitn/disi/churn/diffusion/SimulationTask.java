package it.unitn.disi.churn.diffusion;

import it.unitn.disi.churn.config.ExperimentReader.Experiment;
import it.unitn.disi.churn.config.YaoChurnConfigurator;
import it.unitn.disi.churn.diffusion.churn.CachingTransformer;
import it.unitn.disi.churn.diffusion.churn.LiveTransformer;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.network.churn.yao.YaoInit.IDistributionGenerator;
import it.unitn.disi.simulator.CyclicProtocolRunner;
import it.unitn.disi.simulator.CyclicSchedulable;
import it.unitn.disi.simulator.FixedProcess;
import it.unitn.disi.simulator.IEventObserver;
import it.unitn.disi.simulator.IProcess;
import it.unitn.disi.simulator.IProcess.State;
import it.unitn.disi.simulator.PausingCyclicProtocolRunner;
import it.unitn.disi.simulator.RenewalProcess;
import it.unitn.disi.simulator.SimpleEDSim;
import it.unitn.disi.utils.collections.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;

public class SimulationTask implements Callable<HFlood[]> {

	private final static int HFLOOD_PID = 0;

	private final double fBurnin;

	private final double fPeriod;

	private final Experiment fExperiment;

	private final int fSource;

	private final IndexedNeighborGraph fGraph;

	private final Random fRandom;

	private final YaoChurnConfigurator fYaoConf;

	private final String fPeerSelector;

	private HFlood[] fProtocols;

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
	public HFlood[] call() throws Exception {

		fProtocols = protocols(fGraph, fRandom);

		IProcess[] processes = processes(fExperiment, fSource, fGraph, fRandom,
				fProtocols);

		List<Pair<Integer, ? extends IEventObserver>> observers = new ArrayList<Pair<Integer, ? extends IEventObserver>>();

		HFlood source = fProtocols[fSource];

		// Triggers the dissemination process from the first login of the
		// sender.
		observers
				.add(new Pair<Integer, IEventObserver>(
						IProcess.PROCESS_SCHEDULABLE_TYPE, source
								.sourceEventObserver()));

		CyclicProtocolRunner<HFlood> cpr;

		if (!staticExperiment(fExperiment)) {
			PausingCyclicProtocolRunner<HFlood> pcpr = new PausingCyclicProtocolRunner<HFlood>(
					fPeriod, 1, HFLOOD_PID);

			// Resumes the cyclic protocol whenever the network state changes.
			observers.add(new Pair<Integer, IEventObserver>(
					IProcess.PROCESS_SCHEDULABLE_TYPE, pcpr.networkObserver()));

			cpr = pcpr;
		} else {
			cpr = new CyclicProtocolRunner<HFlood>(HFLOOD_PID);
		}

		// Cyclic protocol observer.
		observers.add(new Pair<Integer, IEventObserver>(1, cpr));

		SimpleEDSim bcs = new SimpleEDSim(processes, observers, fBurnin);
		
		if (staticExperiment(fExperiment)) {
			bcs.schedule(new CyclicSchedulable(fPeriod, 1));
		}
		
		bcs.run();

		return fProtocols;
	}

	private IProcess[] processes(Experiment experiment, int source,
			IndexedNeighborGraph graph, Random r, HFlood[] protos) {

		IProcess[] rp = new IProcess[graph.size()];

		for (int i = 0; i < rp.length; i++) {
			Object[] pArray = new Object[] { protos[i] };
			if (staticExperiment(experiment)) {
				rp[i] = new FixedProcess(i, State.up, pArray);
			} else {
				IDistributionGenerator dgen = fYaoConf.distributionGenerator();
				rp[i] = new RenewalProcess(i,
						dgen.uptimeDistribution(experiment.lis[i]),
						dgen.downtimeDistribution(experiment.dis[i]),
						State.down, pArray);
			}
		}
		return rp;
	}

	private boolean staticExperiment(Experiment experiment) {
		return experiment.lis == null;
	}

	private HFlood[] protocols(IndexedNeighborGraph graph, Random r) {
		HFlood[] protos = new HFlood[graph.size()];
		CachingTransformer caching = new CachingTransformer(
				new LiveTransformer());

		for (int i = 0; i < graph.size(); i++) {
			protos[i] = new HFlood(graph, peerSelector(r), caching, i,
					HFLOOD_PID);
		}

		return protos;
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
