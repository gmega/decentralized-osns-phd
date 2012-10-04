package it.unitn.disi.churn.connectivity;

import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.simulator.churnmodel.yao.YaoChurnConfigurator;
import it.unitn.disi.simulator.concurrent.SimulationTask;
import it.unitn.disi.simulator.core.EDSimulationEngine;
import it.unitn.disi.simulator.core.IProcess;
import it.unitn.disi.simulator.core.IEventObserver;
import it.unitn.disi.simulator.measure.IMetricAccumulator;
import it.unitn.disi.simulator.measure.INodeMetric;
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

	private ComplexTCE fLast;

	private IProcess[] fProcesses;

	private HashMap<Integer, List<INodeMetric<Double>>> fMap = new HashMap<Integer, List<INodeMetric<Double>>>();

	public SimulationTaskBuilder(IndexedNeighborGraph graph, int[] ids, int root) {
		fSims = new ArrayList<Pair<Integer, ? extends IEventObserver>>();
		fGraph = graph;
		fIds = ids;
		fRoot = root;
	}

	public void createProcesses(double[] lIs, double[] dIs,
			YaoChurnConfigurator conf) {
		fProcesses = conf.createProcesses(lIs, dIs, dIs.length);
	}

	public SimulationTaskBuilder addConnectivitySimulation(int source,
			int[] fixedNodes, ActivationSampler sampler) {
		final ComplexTCE tce = new ComplexTCE(fGraph, source, fixedNodes,
				sampler);
		fLast = tce;
		addSim(fLast);

		addMetric(source, new INodeMetric<Double>() {
			@Override
			public Object id() {
				return "ed";
			}

			@Override
			public Double getMetric(int i) {
				return tce.reachTime(i);
			}
		});

		addMetric(source, new INodeMetric<Double>() {
			@Override
			public Object id() {
				return "rd";
			}

			@Override
			public Double getMetric(int i) {
				return tce.perceivedDelay(i);
			}
		});

		return this;
	}

	public SimulationTaskBuilder addMultiConnectivitySimulation(int source,
			final int repeats, IMetricAccumulator<Double> accumulator) {
		final MultiTCE multi = new MultiTCE(fGraph, source, repeats, accumulator);
		addSim(multi);
		addMetric(source, accumulator);
		
		return this;
	}

	public void addCloudSimulation(int source) {
		final CloudSim sim = new CloudSim(source, fGraph.size());
		addSim(sim);
		addMetric(source, new INodeMetric<Double>() {
			@Override
			public Object id() {
				return "cloud_delay";
			}

			@Override
			public Double getMetric(int i) {
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

	private void addMetric(int source, INodeMetric<Double> metric) {
		List<INodeMetric<Double>> list = fMap.get(source);
		if (list == null) {
			list = new ArrayList<INodeMetric<Double>>();
			fMap.put(source, list);
		}

		list.add(metric);
	}

	public SimulationTask simulationTask(double burnIn) {
		@SuppressWarnings("unchecked")
		Pair<Integer, List<? extends INodeMetric<? extends Object>>>[] metrics = new Pair[fMap
				.size()];
		int k = 0;
		for (int key : fMap.keySet()) {
			metrics[k++] = new Pair<Integer, List<? extends INodeMetric<? extends Object>>>(
					key, fMap.get(key));
		}

		Arrays.sort(
				metrics,
				new Comparator<Pair<Integer, List<? extends INodeMetric<? extends Object>>>>() {

					@Override
					public int compare(
							Pair<Integer, List<? extends INodeMetric<? extends Object>>> o1,
							Pair<Integer, List<? extends INodeMetric<? extends Object>>> o2) {
						return o1.a - o2.a;
					}
				});

		EDSimulationEngine engine = new EDSimulationEngine(fProcesses, burnIn);
		engine.setEventObservers(fSims);

		return new SimulationTask(null, engine, null, metrics);
	}
}
