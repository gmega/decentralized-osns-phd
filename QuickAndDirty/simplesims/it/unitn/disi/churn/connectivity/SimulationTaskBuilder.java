package it.unitn.disi.churn.connectivity;

import it.unitn.disi.churn.config.YaoChurnConfigurator;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.simulator.IEventObserver;
import it.unitn.disi.simulator.IProcess;
import it.unitn.disi.utils.collections.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class SimulationTaskBuilder {

	private final ArrayList<Pair<Integer, ? extends IEventObserver>> fSims;

	private final IndexedNeighborGraph fGraph;

	private final int[] fIds;
	
	private final int fRoot;

	private TemporalConnectivityEstimator fLast;

	private HashMap<Integer, List<INetworkMetric>> fMap = new HashMap<Integer, List<INetworkMetric>>();

	public SimulationTaskBuilder(IndexedNeighborGraph graph, int[] ids, int root) {
		fSims = new ArrayList<Pair<Integer, ? extends IEventObserver>>();
		fGraph = graph;
		fIds = ids;
		fRoot = root;
	}

	public SimulationTaskBuilder addConnectivitySimulation(int source,
			int[] fixedNodes, ActivationSampler sampler) {
		final TemporalConnectivityEstimator tce = new TemporalConnectivityEstimator(
				fGraph, source, fixedNodes, sampler);
		fLast = tce;
		addSim(fLast);

		addMetric(source, new INetworkMetric() {
			@Override
			public Object id() {
				return "ed";
			}

			@Override
			public double getMetric(int i) {
				return tce.reachTime(i);
			}
		});

		addMetric(source, new INetworkMetric() {
			@Override
			public Object id() {
				return "rd";
			}

			@Override
			public double getMetric(int i) {
				return tce.perceivedDelay(i);
			}
		});

		return this;
	}

	public void addCloudSimulation(int source) {
		final CloudSim sim = new CloudSim(source);
		addSim(sim);
		addMetric(source, new INetworkMetric() {
			@Override
			public Object id() {
				return "cloud_delay";
			}

			@Override
			public double getMetric(int i) {
				return sim.reachTime(i);
			}
		});
	}

	public void andComponentTracker() {
		if (fLast == null) {
			throw new IllegalStateException();
		}
		addSim(new ComponentTracker(fLast, fGraph, System.out, fRoot,
				fIds[fLast.source()], fLast.source()));
	}

	private void addSim(IEventObserver observer) {
		fSims.add(new Pair<Integer, IEventObserver>(
				IProcess.PROCESS_SCHEDULABLE_TYPE, observer));
	}

	private void addMetric(int source, INetworkMetric metric) {
		List<INetworkMetric> list = fMap.get(source);
		if (list == null) {
			list = new ArrayList<INetworkMetric>();
			fMap.put(source, list);
		}

		list.add(metric);
	}

	public SimulationTask simulationTask(double[] lIs, double[] dIs,
			double burnIn, YaoChurnConfigurator conf) {
		@SuppressWarnings("unchecked")
		Pair<Integer, List<INetworkMetric>>[] metrics = new Pair[fMap.size()];
		int k = 0;
		for (int key : fMap.keySet()) {
			metrics[k++] = new Pair<Integer, List<INetworkMetric>>(key,
					fMap.get(key));
		}

		Arrays.sort(metrics,
				new Comparator<Pair<Integer, List<INetworkMetric>>>() {

					@Override
					public int compare(Pair<Integer, List<INetworkMetric>> o1,
							Pair<Integer, List<INetworkMetric>> o2) {
						return o1.a - o2.a;
					}
				});

		return new SimulationTask(lIs, dIs, burnIn, fGraph, conf, fSims,
				metrics);
	}
}
