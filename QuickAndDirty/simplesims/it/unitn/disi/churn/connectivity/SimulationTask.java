package it.unitn.disi.churn.connectivity;

import it.unitn.disi.churn.BaseChurnSim;
import it.unitn.disi.churn.RenewalProcess;
import it.unitn.disi.churn.YaoChurnConfigurator;
import it.unitn.disi.churn.RenewalProcess.State;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.network.churn.yao.YaoInit.IDistributionGenerator;
import it.unitn.disi.utils.collections.Pair;

import java.util.ArrayList;
import java.util.concurrent.Callable;

public class SimulationTask implements Callable<Pair<Integer, double[]>[]> {

	public static final int LI = 0;

	public static final int DI = 1;

	private final double[][] fld;

	private final int fSourceStart;

	private final int fSourceEnd;

	private final double fBurnin;

	private final IndexedNeighborGraph fGraph;

	private final ActivationSampler fSampler;

	private final YaoChurnConfigurator fYaoConf;

	public SimulationTask(double[][] ld, int start, int end, double burnin,
			IndexedNeighborGraph graph, ActivationSampler sampler,
			YaoChurnConfigurator yaoConf) {
		fld = ld;
		fSourceStart = start;
		fSourceEnd = end;
		fGraph = graph;
		fSampler = sampler;
		fBurnin = burnin;
		fYaoConf = yaoConf;
	}

	@Override
	public Pair<Integer, double[]>[] call() throws Exception {

		RenewalProcess[] rp = new RenewalProcess[fGraph.size()];
		IDistributionGenerator distGen = fYaoConf.distributionGenerator();

		for (int i = 0; i < rp.length; i++) {
			rp[i] = new RenewalProcess(i,
					distGen.uptimeDistribution(fld[i][LI]),
					distGen.downtimeDistribution(fld[i][DI]), State.down);
		}

		ArrayList<TemporalConnectivityEstimator> sims = new ArrayList<TemporalConnectivityEstimator>();
		for (int i = fSourceStart; i <= fSourceEnd; i++) {
			if (fSourceEnd - fSourceStart > 0 && fSampler != null) {
				throw new InternalError(
						"FIXME: activation sampling is broken with multiple sources.");
			}
			TemporalConnectivityEstimator tce = new TemporalConnectivityEstimator(
					fGraph, i, fSampler);
			sims.add(tce);
		}

		BaseChurnSim bcs = new BaseChurnSim(rp, sims, fBurnin);
		bcs.run();

		@SuppressWarnings("unchecked")
		Pair<Integer, double[]>[] results = new Pair[sims.size()];
		for (int i = 0; i <= sims.size(); i++) {
			results[i] = getResults(sims.get(i), fSourceStart + i);
		}

		return results;
	}

	private Pair<Integer, double[]> getResults(
			TemporalConnectivityEstimator tce, int source) {
		double[] contrib = new double[fGraph.size()];
		for (int i = 0; i < contrib.length; i++) {
			contrib[i] = tce.reachTime(i);
		}
		return new Pair<Integer, double[]>(source, contrib);
	}

}
