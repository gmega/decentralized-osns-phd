package it.unitn.disi.churn.diffusion.experiments.config;

import it.unitn.disi.churn.config.Experiment;
import it.unitn.disi.churn.diffusion.HFloodMMsg;
import it.unitn.disi.churn.diffusion.HFloodSM;
import it.unitn.disi.churn.diffusion.IPeerSelector;
import it.unitn.disi.churn.diffusion.IProtocolReference;
import it.unitn.disi.churn.diffusion.MessageStatistics;
import it.unitn.disi.churn.diffusion.PIDReference;
import it.unitn.disi.churn.diffusion.graph.CachingTransformer;
import it.unitn.disi.churn.diffusion.graph.ILiveTransformer;
import it.unitn.disi.churn.diffusion.graph.LiveTransformer;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.simulator.core.EDSimulationEngine;
import it.unitn.disi.simulator.core.EngineBuilder;
import it.unitn.disi.simulator.core.IClockData;
import it.unitn.disi.simulator.core.IProcess;
import it.unitn.disi.simulator.core.IProcess.State;
import it.unitn.disi.simulator.measure.INodeMetric;
import it.unitn.disi.simulator.protocol.CyclicProtocolRunner;
import it.unitn.disi.simulator.protocol.CyclicSchedulable;
import it.unitn.disi.simulator.protocol.FixedProcess;
import it.unitn.disi.utils.collections.Pair;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class StaticSimulationBuilder {

	public static final int HFLOOD_PID = 0;

	protected HFloodSM[] fProtocols;

	public Pair<EDSimulationEngine, List<INodeMetric<? extends Object>>> build(
			double period, Experiment experiment, int root, int source,
			IndexedNeighborGraph graph, IPeerSelector[] selectors) {

		EngineBuilder builder = new EngineBuilder();
		
		IProcess processes[] = new IProcess[graph.size()];
		for (int i = 0; i < processes.length; i++) {
			processes[i] = new FixedProcess(i, State.up);
		}
		
		builder.addProcess(processes);

		MessageStatistics stats = new MessageStatistics("msg", graph.size(),
				true);
		fProtocols = protocols(graph, root, new CachingTransformer(
				new LiveTransformer()), processes, selectors, stats);

		CyclicProtocolRunner<HFloodSM> cpr = new CyclicProtocolRunner<HFloodSM>(
				HFLOOD_PID);

		// Cyclic protocol observer.
		builder.addObserver(cpr, 1, true, true);
		builder.preschedule(new CyclicSchedulable(period, 1));

		EDSimulationEngine engine = builder.engine();
		
		HFloodMMsg msg = new HFloodMMsg(0.0, 0);
		for (int i = 0; i < fProtocols.length; i++) {
			fProtocols[i].setMessage(msg, null);
		}
		
		fProtocols[0].markReached(0, engine.clock(), 0);

		stats.startTrackingSession(engine.clock());

		List<INodeMetric<? extends Object>> metrics = new ArrayList<INodeMetric<? extends Object>>();
		metrics.add(SMMetrics.rdMetric(source, fProtocols));
		metrics.addAll(stats.metrics());

		return new Pair<EDSimulationEngine, List<INodeMetric<? extends Object>>>(
				engine, metrics);
	}

	protected HFloodSM[] protocols(IndexedNeighborGraph graph, int root,
			ILiveTransformer transformer, IProcess[] processes,
			IPeerSelector[] selectors, final MessageStatistics stats) {
		fProtocols = new HFloodSM[graph.size()];

		IProtocolReference<HFloodSM> ref = new PIDReference<HFloodSM>(
				HFLOOD_PID);

		for (int i = 0; i < graph.size(); i++) {
			fProtocols[i] = new HFloodSM(graph, selectors[i], processes[i],
					transformer, ref) {
				@Override
				protected void messageReceived(int sender, IClockData clock,
						int flags) {
					stats.messageReceived(sender, id(), (HFloodMMsg) message(),
							clock, flags);
				}

			};
			processes[i].addProtocol(fProtocols[i]);
		}

		return fProtocols;
	}

}
