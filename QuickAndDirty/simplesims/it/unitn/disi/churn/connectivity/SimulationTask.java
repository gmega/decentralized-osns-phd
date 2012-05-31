package it.unitn.disi.churn.connectivity;

import it.unitn.disi.churn.config.YaoChurnConfigurator;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.network.churn.yao.YaoInit.IDistributionGenerator;
import it.unitn.disi.simulator.IEventObserver;
import it.unitn.disi.simulator.IProcess.State;
import it.unitn.disi.simulator.RenewalProcess;
import it.unitn.disi.simulator.SimpleEDSim;
import it.unitn.disi.simulator.random.IDistribution;
import it.unitn.disi.utils.collections.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

// FIXME Hack for cloud sims.
public class SimulationTask implements
		Callable<Pair<Integer, List<INetworkMetric>>[]> {

	private final double[] fLis;

	private final double[] fDis;

	private final double fBurnin;

	private final IndexedNeighborGraph fGraph;

	private final YaoChurnConfigurator fYaoConf;

	private final List<Pair<Integer, ? extends IEventObserver>> fObservers;

	private final Pair<Integer, List<INetworkMetric>>[] fMetrics;

	public SimulationTask(double[] lIs, double[] dIs,
			double burnin, IndexedNeighborGraph graph,
			YaoChurnConfigurator yaoConf,
			List<Pair<Integer, ? extends IEventObserver>> observers,
			Pair<Integer, List<INetworkMetric>>[] metrics) {
		fLis = lIs;
		fDis = dIs;
		fGraph = graph;
		fBurnin = burnin;
		fYaoConf = yaoConf;
		fObservers = observers;
		fMetrics = metrics;
	}

	@Override
	public Pair<Integer, List<INetworkMetric>>[] call() throws Exception {

		RenewalProcess[] rp = new RenewalProcess[fGraph.size()];
		IDistributionGenerator distGen = fYaoConf.distributionGenerator();

		for (int i = 0; i < rp.length; i++) {
			rp[i] = new RenewalProcess(i, uptimeDistribution(distGen, fLis[i]),
					downtimeDistribution(distGen, fDis[i]), State.down);
		}

		SimpleEDSim bcs = new SimpleEDSim(rp, fObservers, fBurnin);
		bcs.run();

		return fMetrics;
	}

	private IDistribution downtimeDistribution(IDistributionGenerator distGen,
			double d) {
		return distGen.downtimeDistribution(d);
	}

	private IDistribution uptimeDistribution(IDistributionGenerator distGen,
			double d) {
		return distGen.uptimeDistribution(d);
	}

}
