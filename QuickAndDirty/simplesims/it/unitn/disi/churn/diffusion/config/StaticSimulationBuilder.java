package it.unitn.disi.churn.diffusion.config;

import it.unitn.disi.churn.config.ExperimentReader.Experiment;
import it.unitn.disi.churn.diffusion.HFlood;
import it.unitn.disi.churn.diffusion.graph.CachingTransformer;
import it.unitn.disi.churn.diffusion.graph.LiveTransformer;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.simulator.core.EDSimulationEngine;
import it.unitn.disi.simulator.core.IProcess;
import it.unitn.disi.simulator.core.IProcess.State;
import it.unitn.disi.simulator.core.ISimulationObserver;
import it.unitn.disi.simulator.measure.INetworkMetric;
import it.unitn.disi.simulator.protocol.CyclicProtocolRunner;
import it.unitn.disi.simulator.protocol.CyclicSchedulable;
import it.unitn.disi.simulator.protocol.FixedProcess;
import it.unitn.disi.utils.collections.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class StaticSimulationBuilder extends DiffusionSimulationBuilder {

	public Pair<EDSimulationEngine, List<INetworkMetric>> build(double burnin,
			double period, Experiment experiment, int source,
			String peerSelector, IndexedNeighborGraph graph, Random random) throws Exception {

		IProcess processes [] = new IProcess[graph.size()];
		for (int i = 0; i < processes.length; i++) {
			processes[i] = new FixedProcess(i, State.up);
		}
		
		fProtocols = protocols(graph, random, peerSelector,
				new CachingTransformer(new LiveTransformer()), processes);

		List<Pair<Integer, ? extends ISimulationObserver>> observers = new ArrayList<Pair<Integer, ? extends ISimulationObserver>>();

		HFlood sourceProtocol = fProtocols[source];

		// Triggers the dissemination process from the first login of the
		// sender.
		observers.add(new Pair<Integer, ISimulationObserver>(
				IProcess.PROCESS_SCHEDULABLE_TYPE, sourceProtocol
						.sourceEventObserver()));

		CyclicProtocolRunner<HFlood> cpr = new CyclicProtocolRunner<HFlood>(
				HFLOOD_PID);

		// Cyclic protocol observer.
		observers.add(new Pair<Integer, ISimulationObserver>(1, cpr));

		EDSimulationEngine bcs = new EDSimulationEngine(processes, observers, 0.0);
		bcs.schedule(new CyclicSchedulable(period, 1));
		
		List<INetworkMetric> metrics = new ArrayList<INetworkMetric>();
		addEDMetric(metrics, fProtocols, source);

		return new Pair<EDSimulationEngine, List<INetworkMetric>>(bcs, metrics);
	}

}
