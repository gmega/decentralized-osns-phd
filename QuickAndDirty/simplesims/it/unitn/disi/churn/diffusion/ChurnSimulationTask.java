package it.unitn.disi.churn.diffusion;

import it.unitn.disi.churn.config.ExperimentReader.Experiment;
import it.unitn.disi.churn.config.YaoChurnConfigurator;
import it.unitn.disi.churn.diffusion.graph.CachingTransformer;
import it.unitn.disi.churn.diffusion.graph.LiveTransformer;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.network.churn.yao.YaoInit.IDistributionGenerator;
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

public class ChurnSimulationTask extends DiffusionSimulationTask {

	protected final double fBurnin;

	protected final double fPeriod;

	protected final Experiment fExperiment;

	protected final int fSource;

	protected final IndexedNeighborGraph fGraph;

	protected final Random fRandom;

	protected final YaoChurnConfigurator fYaoConf;

	protected final String fPeerSelector;

	public ChurnSimulationTask(double burnin, double period,
			Experiment experiment, YaoChurnConfigurator yaoConf, int source,
			String peerSelector, IndexedNeighborGraph graph, Random random) {
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
	public ChurnSimulationTask call() throws Exception {

		fProtocols = protocols(fGraph, fRandom, fPeerSelector,
				new CachingTransformer(new LiveTransformer()));

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

		PausingCyclicProtocolRunner<HFlood> pcpr = new PausingCyclicProtocolRunner<HFlood>(
				fPeriod, 1, HFLOOD_PID);

		// Resumes the cyclic protocol whenever the network state changes.
		observers.add(new Pair<Integer, IEventObserver>(
				IProcess.PROCESS_SCHEDULABLE_TYPE, pcpr.networkObserver()));

		// Cyclic protocol observer.
		observers.add(new Pair<Integer, IEventObserver>(1, pcpr));

		extraObservers(observers);

		SimpleEDSim bcs = new SimpleEDSim(processes, observers, fBurnin);
		otherConfig(bcs, pcpr);

		bcs.run();

		return this;
	}
	
	public HFlood[] protocols(){
		return fProtocols;
	}

	protected void extraObservers(
			List<Pair<Integer, ? extends IEventObserver>> observers) {
		// To be overridden by subclasses.
	}

	protected void otherConfig(SimpleEDSim sim,
			PausingCyclicProtocolRunner<HFlood> scheduler) {
		// To be overridden by subclasses.
	}

	@Override
	protected IProcess create(int i, Experiment experiment, Object[] pArray) {
		IDistributionGenerator dgen = fYaoConf.distributionGenerator();
		return new RenewalProcess(i,
				dgen.uptimeDistribution(experiment.lis[i]),
				dgen.downtimeDistribution(experiment.dis[i]), State.down,
				pArray);
	}
}
