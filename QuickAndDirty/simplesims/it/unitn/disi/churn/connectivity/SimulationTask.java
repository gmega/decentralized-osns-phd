package it.unitn.disi.churn.connectivity;

import it.unitn.disi.churn.config.YaoChurnConfigurator;
import it.unitn.disi.churn.simulator.IEventObserver;
import it.unitn.disi.churn.simulator.IProcess;
import it.unitn.disi.churn.simulator.IProcess.State;
import it.unitn.disi.churn.simulator.RenewalProcess;
import it.unitn.disi.churn.simulator.SimpleEDSim;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.network.churn.yao.YaoInit.IDistributionGenerator;
import it.unitn.disi.random.IDistribution;
import it.unitn.disi.utils.collections.Pair;

import java.util.ArrayList;
import java.util.concurrent.Callable;

// FIXME Hack for cloud sims.
public class SimulationTask implements Callable<SimulationResults[]> {

	private final double[] fLis;

	private final double[] fDis;

	private final int[] fCloudNodes;

	private final int fSourceStart;

	private final int fSourceEnd;

	private final double fBurnin;

	private final boolean fCloud;

	private final IndexedNeighborGraph fGraph;

	private final ActivationSampler fSampler;

	private final YaoChurnConfigurator fYaoConf;

	public SimulationTask(double[] lIs, double[] dIs, int[] cloudNodes,
			int start, int end, double burnin, boolean cloudSim,
			IndexedNeighborGraph graph, ActivationSampler sampler,
			YaoChurnConfigurator yaoConf) {
		fLis = lIs;
		fDis = dIs;
		fCloudNodes = cloudNodes;
		fSourceStart = start;
		fSourceEnd = end;
		fGraph = graph;
		fSampler = sampler;
		fBurnin = burnin;
		fYaoConf = yaoConf;
		fCloud = cloudSim;
	}

	@Override
	public SimulationResults[] call() throws Exception {

		RenewalProcess[] rp = new RenewalProcess[fGraph.size()];
		IDistributionGenerator distGen = fYaoConf.distributionGenerator();

		for (int i = 0; i < rp.length; i++) {
			rp[i] = new RenewalProcess(i, uptimeDistribution(distGen, fLis[i]),
					downtimeDistribution(distGen, fDis[i]), State.down);
		}

		ArrayList<TemporalConnectivityEstimator> tceSims = new ArrayList<TemporalConnectivityEstimator>();
		ArrayList<CloudSim> cloudSims = new ArrayList<CloudSim>();

		for (int i = fSourceStart; i <= fSourceEnd; i++) {
			if (fSourceEnd - fSourceStart > 0 && fSampler != null) {
				throw new InternalError(
						"FIXME: activation sampling is broken with multiple sources.");
			}

			TemporalConnectivityEstimator tce = new TemporalConnectivityEstimator(
					fGraph, i, fCloudNodes, fSampler);
			tceSims.add(tce);

			CloudSim cs = new CloudSim(i);
			cloudSims.add(cs);
		}

		ArrayList<Pair<Integer, ? extends IEventObserver>> allsims = new ArrayList<Pair<Integer, ? extends IEventObserver>>();
		addAll(allsims, tceSims);

		if (fCloud) {
			addAll(allsims, cloudSims);
		}

		SimpleEDSim bcs = new SimpleEDSim(rp, allsims, fBurnin);
		bcs.run();

		SimulationResults[] results = new SimulationResults[tceSims.size()];
		for (int i = 0; i < tceSims.size(); i++) {
			results[i] = getResults(tceSims.get(i), cloudSims.get(i),
					fSourceStart + i);
		}

		return results;
	}

	private void addAll(
			ArrayList<Pair<Integer, ? extends IEventObserver>> allsims,
			ArrayList<? extends IEventObserver> sims) {
		for (IEventObserver observer : sims) {
			allsims.add(new Pair<Integer, IEventObserver>(
					IProcess.PROCESS_SCHEDULABLE_TYPE, observer));
		}
	}

	private IDistribution downtimeDistribution(IDistributionGenerator distGen,
			double d) {
		return distGen.downtimeDistribution(d);
	}

	private IDistribution uptimeDistribution(IDistributionGenerator distGen,
			double d) {
		return distGen.uptimeDistribution(d);
	}

	private SimulationResults getResults(TemporalConnectivityEstimator tce,
			CloudSim cs, int source) {
		double[] tceResult = new double[fGraph.size()];
		double[] pdResult = new double[fGraph.size()];
		double[] csResult = new double[fGraph.size()];

		for (int i = 0; i < tceResult.length; i++) {
			tceResult[i] = tce.reachTime(i);
			pdResult[i] = tce.perceivedDelay(i);
			if (fCloud) {
				csResult[i] = cs.reachTime(i);
			}
		}
		return new SimulationResults(source, tceResult, pdResult, csResult);
	}

}
