package it.unitn.disi.churn.diffusion.experiments.config;

import it.unitn.disi.churn.config.Experiment;
import it.unitn.disi.churn.diffusion.BiasedCentralitySelector;
import it.unitn.disi.churn.diffusion.ComponentSelector;
import it.unitn.disi.churn.diffusion.HFloodSM;
import it.unitn.disi.churn.diffusion.IPeerSelector;
import it.unitn.disi.churn.diffusion.IProtocolReference;
import it.unitn.disi.churn.diffusion.PIDReference;
import it.unitn.disi.churn.diffusion.RandomSelector;
import it.unitn.disi.churn.diffusion.graph.CachingTransformer;
import it.unitn.disi.churn.diffusion.graph.ILiveTransformer;
import it.unitn.disi.churn.diffusion.graph.LiveTransformer;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.simulator.core.EDSimulationEngine;
import it.unitn.disi.simulator.core.EngineBuilder;
import it.unitn.disi.simulator.core.IProcess;
import it.unitn.disi.simulator.core.ISimulationEngine;
import it.unitn.disi.simulator.core.IProcess.State;
import it.unitn.disi.simulator.core.IEventObserver;
import it.unitn.disi.simulator.measure.INodeMetric;
import it.unitn.disi.simulator.protocol.CyclicProtocolRunner;
import it.unitn.disi.simulator.protocol.CyclicSchedulable;
import it.unitn.disi.simulator.protocol.FixedProcess;
import it.unitn.disi.utils.collections.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class StaticSimulationBuilder {

	public static final int HFLOOD_PID = 0;

	protected HFloodSM[] fProtocols;

	public Pair<EDSimulationEngine, List<INodeMetric<? extends Object>>> build(
			double period, Experiment experiment, int root, int source,
			String peerSelector, IndexedNeighborGraph graph, Random random) {

		EngineBuilder builder = new EngineBuilder();
		
		IProcess processes[] = new IProcess[graph.size()];
		for (int i = 0; i < processes.length; i++) {
			processes[i] = new FixedProcess(i, State.up);
		}

		fProtocols = protocols(graph, random, peerSelector, root,
				new CachingTransformer(new LiveTransformer()), processes);

		CyclicProtocolRunner<HFloodSM> cpr = new CyclicProtocolRunner<HFloodSM>(
				HFLOOD_PID);

		// Cyclic protocol observer.
		builder.addObserver(cpr, 1, true, true);
		builder.preschedule(new CyclicSchedulable(period, 1));

		EDSimulationEngine engine = builder.engine();
		
		fProtocols[0].markReached(engine.clock());

		List<INodeMetric<? extends Object>> metrics = new ArrayList<INodeMetric<? extends Object>>();
		metrics.add(SMMetrics.rdMetric(source, fProtocols));

		return new Pair<EDSimulationEngine, List<INodeMetric<? extends Object>>>(
				engine, metrics);
	}

	protected HFloodSM[] protocols(IndexedNeighborGraph graph, Random r,
			String peerSelector, int root, ILiveTransformer transformer,
			IProcess[] processes) {
		fProtocols = new HFloodSM[graph.size()];

		IProtocolReference<HFloodSM> ref = new PIDReference<HFloodSM>(
				HFLOOD_PID);

		for (int i = 0; i < graph.size(); i++) {
			fProtocols[i] = new HFloodSM(graph, peerSelector(i, root, r,
					peerSelector), processes[i], transformer, ref);
			processes[i].addProtocol(fProtocols[i]);
		}

		return fProtocols;
	}

	protected IPeerSelector peerSelector(int peer, int root, Random r,
			String selector) {
		switch (selector.charAt(0)) {
		case 'o':
			if (peer == root) {
				return new ComponentSelector(new BiasedCentralitySelector(r,
						true));
			}
			// Otherwise we use anticentrality.
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
