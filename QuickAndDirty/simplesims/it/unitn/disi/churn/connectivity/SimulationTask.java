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

public class SimulationTask implements Callable<SimulationResults[]> {

	private final double[] fLis;

	private final double[] fDis;

	private final int fSourceStart;

	private final int fSourceEnd;

	private final double fBurnin;

	private final IndexedNeighborGraph fGraph;

	private final ActivationSampler fSampler;

	private final YaoChurnConfigurator fYaoConf;

	public SimulationTask(double[] lIs, double[] dIs, int start, int end,
			double burnin, IndexedNeighborGraph graph,
			ActivationSampler sampler, YaoChurnConfigurator yaoConf) {
		fLis = lIs;
		fDis = dIs;
		fSourceStart = start;
		fSourceEnd = end;
		fGraph = graph;
		fSampler = sampler;
		fBurnin = burnin;
		fYaoConf = yaoConf;
	}

	@Override
	public SimulationResults[] call() throws Exception {

		RenewalProcess[] rp = new RenewalProcess[fGraph.size()];
		IDistributionGenerator distGen = fYaoConf.distributionGenerator();

		for (int i = 0; i < rp.length; i++) {
			rp[i] = new RenewalProcess(i, distGen.uptimeDistribution(fLis[i]),
					distGen.downtimeDistribution(fDis[i]), State.down);
		}

		ArrayList<TemporalConnectivityEstimator> tceSims = new ArrayList<TemporalConnectivityEstimator>();
		ArrayList<CloudSim> cloudSims = new ArrayList<CloudSim>();

		for (int i = fSourceStart; i <= fSourceEnd; i++) {
			if (fSourceEnd - fSourceStart > 0 && fSampler != null) {
				throw new InternalError(
						"FIXME: activation sampling is broken with multiple sources.");
			}
			TemporalConnectivityEstimator tce = new TemporalConnectivityEstimator(
					fGraph, i, fSampler);
			tceSims.add(tce);

			CloudSim cs = new CloudSim(i);
			cloudSims.add(cs);
		}

		ArrayList<IChurnSim> allsims = new ArrayList<IChurnSim>();
		allsims.addAll(tceSims);
		allsims.addAll(cloudSims);

		BaseChurnSim bcs = new BaseChurnSim(rp, allsims, fBurnin);
		bcs.run();

		SimulationResults[] results = new SimulationResults[tceSims.size()];
		for (int i = 0; i < tceSims.size(); i++) {
			results[i] = getResults(tceSims.get(i), cloudSims.get(i),
					fSourceStart + i);
		}

		return results;
	}

	private SimulationResults getResults(TemporalConnectivityEstimator tce,
			CloudSim cs, int source) {
		double[] tceResult = new double[fGraph.size()];
		double[] csResult = new double[fGraph.size()];

		for (int i = 0; i < tceResult.length; i++) {
			tceResult[i] = tce.reachTime(i);
			csResult[i] = cs.reachTime(i);
		}
		return new SimulationResults(source, tceResult, csResult);
	}

}
