package it.unitn.disi.churn.diffusion.cloud;

import java.util.Random;

import peersim.config.IResolver;

import it.unitn.disi.churn.config.ExperimentReader.Experiment;
import it.unitn.disi.churn.config.YaoChurnConfigurator;
import it.unitn.disi.churn.diffusion.ChurnSimulationTask;
import it.unitn.disi.churn.diffusion.HFlood;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.simulator.IProcess;
import it.unitn.disi.simulator.PausingCyclicProtocolRunner;
import it.unitn.disi.simulator.SimpleEDSim;
import it.unitn.disi.simulator.random.IDistribution;
import it.unitn.disi.simulator.random.UniformDistribution;

public class CloudSimulationTask extends ChurnSimulationTask {

	static enum DelayType {
		periodical, random;
	}

	private static final double SECOND = 0.0002777777777777778D;

	private CloudAccessor[] fAccessors;

	private IResolver fResolver;

	private final int fClockType;

	private final DelayType fType;

	public CloudSimulationTask(double burnin, double period,
			Experiment experiment, YaoChurnConfigurator yaoConf, int source,
			String peerSelector, IndexedNeighborGraph graph, Random random,
			IResolver resolver, Long seed, int clockType) {
		super(burnin, period, experiment, yaoConf, source, peerSelector, graph,
				random, seed);

		fResolver = resolver;
		fClockType = clockType;
		fType = DelayType.valueOf(resolver.getString("", "delay_type")
				.toLowerCase());
	}

	@Override
	protected void otherConfig(SimpleEDSim sim,
			PausingCyclicProtocolRunner<HFlood> runner) {
		fAccessors = new CloudAccessor[fProtocols.length];
		for (int i = 0; i < fProtocols.length; i++) {
			IProcess process = sim.process(i);
			fAccessors[i] = new CloudAccessor(distribution(process, i),
					fProtocols[fSource], fProtocols[i], runner, sim, fClockType);
			process.addProcessObserver(fAccessors[i]);
		}
	}

	public CloudAccessor[] accessors() {
		return fAccessors;
	}

	public IDistribution distribution(IProcess process, int index) {

		switch (fType) {
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
}
