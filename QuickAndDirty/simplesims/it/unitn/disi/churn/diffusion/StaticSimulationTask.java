package it.unitn.disi.churn.diffusion;

import it.unitn.disi.churn.config.ExperimentReader.Experiment;
import it.unitn.disi.churn.diffusion.churn.CachingTransformer;
import it.unitn.disi.churn.diffusion.churn.LiveTransformer;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.simulator.CyclicProtocolRunner;
import it.unitn.disi.simulator.CyclicSchedulable;
import it.unitn.disi.simulator.FixedProcess;
import it.unitn.disi.simulator.IEventObserver;
import it.unitn.disi.simulator.IProcess;
import it.unitn.disi.simulator.SimpleEDSim;
import it.unitn.disi.simulator.IProcess.State;
import it.unitn.disi.utils.collections.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class StaticSimulationTask extends DiffusionSimulationTask {

	private final Experiment fExperiment;

	private final int fSource;

	private final double fPeriod;

	private final Random fRandom;

	private final IndexedNeighborGraph fGraph;

	private final String fPeerSelector;

	private HFlood[] fProtocols;

	public StaticSimulationTask(double burnin, double period,
			Experiment experiment, int source, String peerSelector,
			IndexedNeighborGraph graph, Random random) {
		fPeriod = period;
		fExperiment = experiment;
		fSource = source;
		fGraph = graph;
		fRandom = random;
		fPeerSelector = peerSelector;
	}

	@Override
	public DiffusionSimulationTask call() throws Exception {

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

		CyclicProtocolRunner<HFlood> cpr = new CyclicProtocolRunner<HFlood>(
				HFLOOD_PID);

		// Cyclic protocol observer.
		observers.add(new Pair<Integer, IEventObserver>(1, cpr));

		SimpleEDSim bcs = new SimpleEDSim(processes, observers, 0.0);
		bcs.schedule(new CyclicSchedulable(fPeriod, 1));
		bcs.run();

		return this;
	}

	@Override
	protected IProcess create(int i, Experiment exp, Object[] pArray) {
		return new FixedProcess(i, State.up, pArray);
	}
}
