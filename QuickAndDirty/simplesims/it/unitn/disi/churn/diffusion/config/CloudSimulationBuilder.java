package it.unitn.disi.churn.diffusion.config;

import it.unitn.disi.churn.config.ExperimentReader.Experiment;
import it.unitn.disi.churn.diffusion.HFlood;
import it.unitn.disi.churn.diffusion.cloud.CloudAccessor;
import it.unitn.disi.churn.diffusion.cloud.FixedPeriodDistribution;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.simulator.core.EDSimulationEngine;
import it.unitn.disi.simulator.core.IProcess;
import it.unitn.disi.simulator.measure.INodeMetric;
import it.unitn.disi.simulator.protocol.PausingCyclicProtocolRunner;
import it.unitn.disi.simulator.random.IDistribution;
import it.unitn.disi.simulator.random.UniformDistribution;
import it.unitn.disi.utils.collections.Pair;

import java.util.List;
import java.util.Random;

import peersim.config.IResolver;

public class CloudSimulationBuilder extends ChurnSimulationBuilder {

	static enum DelayType {
		periodical, random;
	}

	private static final double SECOND = 0.0002777777777777778D;

	private CloudAccessor[] fAccessors;

	private int fClockType;

	private DelayType fDelayType;

	private IResolver fResolver;

	private Random fRandom;

	public Pair<EDSimulationEngine, List<INodeMetric<? extends Object>>> build(double burnin,
			double period, Experiment experiment, int source,
			String peerSelector, IndexedNeighborGraph graph, Random random,
			IResolver resolver, int clockType, IProcess[] processes)
			throws Exception {

		fRandom = random;
		fClockType = clockType;
		fResolver = resolver;
		fDelayType = DelayType.valueOf(resolver.getString("", "delay_type")
				.toLowerCase());

		return super.build(burnin, period, experiment, source, peerSelector,
				graph, random, processes);
	}

	@Override
	protected void postBuildEngineConfig(EDSimulationEngine sim,
			PausingCyclicProtocolRunner<HFlood> runner, int source) {
		fAccessors = new CloudAccessor[fProtocols.length];
		for (int i = 0; i < fProtocols.length; i++) {
			IProcess process = sim.process(i);
			fAccessors[i] = new CloudAccessor(distribution(process, i,
					fDelayType), fProtocols[source], fProtocols[i], runner,
					sim, fClockType);
			process.addProcessObserver(fAccessors[i]);
		}
	}

	protected void addMetrics(List<INodeMetric<?>> metric) {
		metric.add(new INodeMetric<Integer>() {

			@Override
			public Object id() {
				return "outcome";
			}

			@Override
			public Integer getMetric(int i) {
				return fAccessors[i].outcome();
			}
		});
	}

	public CloudAccessor[] accessors() {
		return fAccessors;
	}

	public IDistribution distribution(IProcess process, int index,
			DelayType type) {

		switch (type) {
		case periodical:
			double period = fResolver.getDouble("", "delay");
			return new FixedPeriodDistribution(fRandom.nextDouble() * period,
					period);
		case random:
			return new UniformDistribution(fRandom, 0.0, fProtocols.length
					* SECOND);
		}

		throw new IllegalStateException();
	}

	@Override
	protected boolean shouldTrackCore() {
		return false;
	}
}
