package it.unitn.disi.churn.diffusion.experiments.config;

import it.unitn.disi.churn.config.ExperimentReader.Experiment;
import it.unitn.disi.churn.diffusion.CoreTracker;
import it.unitn.disi.churn.diffusion.DiffusionWick;
import it.unitn.disi.churn.diffusion.HFloodSM;
import it.unitn.disi.churn.diffusion.graph.CachingTransformer;
import it.unitn.disi.churn.diffusion.graph.LiveTransformer;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.simulator.core.EDSimulationEngine;
import it.unitn.disi.simulator.core.IProcess;
import it.unitn.disi.simulator.core.IEventObserver;
import it.unitn.disi.simulator.measure.INodeMetric;
import it.unitn.disi.simulator.protocol.PausingCyclicProtocolRunner;
import it.unitn.disi.utils.collections.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ChurnSimulationBuilder extends DiffusionSimulationBuilder {

	private DiffusionWick fControl;

	public Pair<EDSimulationEngine, List<INodeMetric<? extends Object>>> build(
			double burnin, double period, Experiment experiment, int source,
			String peerSelector, IndexedNeighborGraph graph, Random random,
			IProcess[] processes) {

		EDSimulationEngine bcs = new EDSimulationEngine(processes, burnin);

		fProtocols = protocols(graph, random, peerSelector,
				new CachingTransformer(new LiveTransformer()), processes);

		List<Pair<Integer, ? extends IEventObserver>> observers = new ArrayList<Pair<Integer, ? extends IEventObserver>>();

		// Triggers the dissemination process from the first login of the
		// sender.
		DiffusionWick dc = new DiffusionWick(source);
		dc.setPoster(dc.new PostSM(fProtocols[source]));
		observers.add(new Pair<Integer, IEventObserver>(
				IProcess.PROCESS_SCHEDULABLE_TYPE, dc));

		// Maintains the initially connected core.
		CoreTracker ct = null;
		if (shouldTrackCore()) {
			ct = new CoreTracker(fProtocols[source], HFLOOD_PID);
			observers.add(new Pair<Integer, IEventObserver>(
					IProcess.PROCESS_SCHEDULABLE_TYPE, ct));
		}

		PausingCyclicProtocolRunner<HFloodSM> pcpr = new PausingCyclicProtocolRunner<HFloodSM>(
				bcs, period, 1, HFLOOD_PID);

		// Resumes the cyclic protocol whenever the network state changes.
		observers.add(new Pair<Integer, IEventObserver>(
				IProcess.PROCESS_SCHEDULABLE_TYPE, pcpr.networkObserver()));

		// Cyclic protocol observer.
		observers.add(new Pair<Integer, IEventObserver>(1, pcpr));
		extraObservers(observers);

		bcs.setEventObservers(observers);
		postBuildEngineConfig(bcs, pcpr, source);

		List<INodeMetric<?>> metrics = new ArrayList<INodeMetric<?>>();
		metrics.add(SMMetrics.edMetric(fProtocols, dc));
		metrics.add(SMMetrics.rdMetric(source, fProtocols));
		if (ct != null) {
			metrics.add(ct);
		}

		addMetrics(metrics);

		return new Pair<EDSimulationEngine, List<INodeMetric<? extends Object>>>(
				bcs, metrics);
	}

	public HFloodSM[] protocols() {
		return fProtocols;
	}

	public DiffusionWick getControl() {
		return fControl;
	}

	protected boolean shouldTrackCore() {
		return true;
	}

	protected void extraObservers(
			List<Pair<Integer, ? extends IEventObserver>> observers) {
		// To be overridden by subclasses.
	}

	protected void postBuildEngineConfig(EDSimulationEngine sim,
			PausingCyclicProtocolRunner<HFloodSM> scheduler, int ousrce) {
		// To be overridden by subclasses.
	}

	protected void addMetrics(List<INodeMetric<?>> metric) {
		// To be overridden by subclasses.
	}

}
