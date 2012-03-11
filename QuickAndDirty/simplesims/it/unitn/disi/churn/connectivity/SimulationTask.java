package it.unitn.disi.churn.connectivity;

import it.unitn.disi.churn.BaseChurnSim;
import it.unitn.disi.churn.IChurnSim;
import it.unitn.disi.churn.RenewalProcess;
import it.unitn.disi.churn.YaoChurnConfigurator;
import it.unitn.disi.churn.RenewalProcess.State;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.network.churn.yao.YaoInit.IDistributionGenerator;

import java.util.ArrayList;
import java.util.concurrent.Callable;

public class SimulationTask implements Callable<double[]> {

	public static final int LI = 0;

	public static final int DI = 1;

	private final double[][] fld;

	private final int fSource;

	private final double fBurnin;

	private final IndexedNeighborGraph fGraph;

	private final ActivationSampler fSampler;

	private final YaoChurnConfigurator fYaoConf;

	public SimulationTask(double[][] ld, int source, double burnin,
			IndexedNeighborGraph graph, ActivationSampler sampler,
			YaoChurnConfigurator yaoConf) {
		fld = ld;
		fSource = source;
		fGraph = graph;
		fSampler = sampler;
		fBurnin = burnin;
		fYaoConf = yaoConf;
	}

	@Override
	public double[] call() throws Exception {

		RenewalProcess[] rp = new RenewalProcess[fGraph.size()];
		IDistributionGenerator distGen = fYaoConf.distributionGenerator();

		for (int i = 0; i < rp.length; i++) {
			rp[i] = new RenewalProcess(i,
					distGen.uptimeDistribution(fld[i][LI]),
					distGen.downtimeDistribution(fld[i][DI]), State.down);
		}

		TemporalConnectivityEstimator tce = new TemporalConnectivityEstimator(
				fGraph, fSource, fSampler);
		ArrayList<IChurnSim> sims = new ArrayList<IChurnSim>();
		sims.add(tce);

		BaseChurnSim bcs = new BaseChurnSim(rp, sims, fBurnin);
		bcs.run();

		double[] contrib = new double[fGraph.size()];
		for (int i = 0; i < contrib.length; i++) {
			contrib[i] = tce.reachTime(i);
		}
		return contrib;
	}

}
