package it.unitn.disi.churn.diffusion.config;

import it.unitn.disi.churn.config.ExperimentReader.Experiment;
import it.unitn.disi.churn.diffusion.HFlood;
import it.unitn.disi.churn.diffusion.graph.CachingTransformer;
import it.unitn.disi.churn.diffusion.graph.LiveTransformer;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.simulator.core.EDSimulationEngine;
import it.unitn.disi.simulator.core.IProcess;
import it.unitn.disi.simulator.core.ISimulationObserver;
import it.unitn.disi.simulator.measure.INetworkMetric;
import it.unitn.disi.simulator.protocol.PausingCyclicProtocolRunner;
import it.unitn.disi.utils.collections.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ChurnSimulationBuilder extends DiffusionSimulationBuilder {

	public Pair<EDSimulationEngine, List<INetworkMetric>> build(double burnin,
			double period, Experiment experiment, int source,
			String peerSelector, IndexedNeighborGraph graph, Random random,
			IProcess[] processes) throws Exception {

		fProtocols = protocols(graph, random, peerSelector,
				new CachingTransformer(new LiveTransformer()), processes);

		List<Pair<Integer, ? extends ISimulationObserver>> observers = new ArrayList<Pair<Integer, ? extends ISimulationObserver>>();
		HFlood sourceProtocol = fProtocols[source];

		// Triggers the dissemination process from the first login of the
		// sender.
		observers.add(new Pair<Integer, ISimulationObserver>(
				IProcess.PROCESS_SCHEDULABLE_TYPE, sourceProtocol
						.sourceEventObserver()));

		PausingCyclicProtocolRunner<HFlood> pcpr = new PausingCyclicProtocolRunner<HFlood>(
				period, 1, HFLOOD_PID);

		// Resumes the cyclic protocol whenever the network state changes.
		observers.add(new Pair<Integer, ISimulationObserver>(
				IProcess.PROCESS_SCHEDULABLE_TYPE, pcpr.networkObserver()));

		// Cyclic protocol observer.
		observers.add(new Pair<Integer, ISimulationObserver>(1, pcpr));
		extraObservers(observers);

		EDSimulationEngine bcs = new EDSimulationEngine(processes, observers, burnin);
		postBuildEngineConfig(bcs, pcpr, source);

		List<INetworkMetric> metrics = new ArrayList<INetworkMetric>();
		addEDMetric(metrics, fProtocols, source);
		addMetrics(metrics);

		return new Pair<EDSimulationEngine, List<INetworkMetric>>(bcs, metrics);
	}

	public HFlood[] protocols() {
		return fProtocols;
	}

	protected void extraObservers(
			List<Pair<Integer, ? extends ISimulationObserver>> observers) {
		// To be overridden by subclasses.
	}

	protected void postBuildEngineConfig(EDSimulationEngine sim,
			PausingCyclicProtocolRunner<HFlood> scheduler, int ousrce) {
		// To be overridden by subclasses.
	}
	
	protected void addMetrics(List<INetworkMetric> metric) {
		// To be overridden by subclasses.
	}
	
}
