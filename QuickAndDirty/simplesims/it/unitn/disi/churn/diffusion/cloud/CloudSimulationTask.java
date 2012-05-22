package it.unitn.disi.churn.diffusion.cloud;

import java.util.Random;

import it.unitn.disi.churn.config.ExperimentReader.Experiment;
import it.unitn.disi.churn.config.YaoChurnConfigurator;
import it.unitn.disi.churn.diffusion.ChurnSimulationTask;
import it.unitn.disi.churn.diffusion.HFlood;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.simulator.PausingCyclicProtocolRunner;
import it.unitn.disi.simulator.SimpleEDSim;
import it.unitn.disi.simulator.random.UniformDistribution;

public class CloudSimulationTask extends ChurnSimulationTask {
	
	private static final double SECOND = 0.0002777777777777778D;

	private CloudAccessor[] fAccessors;

	public CloudSimulationTask(double burnin, double period,
			Experiment experiment, YaoChurnConfigurator yaoConf, int source,
			String peerSelector, IndexedNeighborGraph graph, Random random) {
		super(burnin, period, experiment, yaoConf, source, peerSelector, graph,
				random);
	}

	@Override
	protected void otherConfig(SimpleEDSim sim,
			PausingCyclicProtocolRunner<HFlood> runner) {
		fAccessors = new CloudAccessor[fProtocols.length];
		for (int i = 0; i < fProtocols.length; i++) {
			fAccessors[i] = new CloudAccessor(new UniformDistribution(fRandom,
					0.0, fProtocols.length*SECOND), fProtocols[fSource],
					fProtocols[i], runner, sim);
			sim.process(i).addProcessObserver(fAccessors[i]);
		}
	}

	public CloudAccessor[] accessors() {
		return fAccessors;
	}
}
