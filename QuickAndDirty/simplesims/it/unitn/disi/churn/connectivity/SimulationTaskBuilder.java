package it.unitn.disi.churn.connectivity;

import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.simulator.churnmodel.yao.YaoChurnConfigurator;
import it.unitn.disi.simulator.concurrent.SimulationTask;
import it.unitn.disi.simulator.core.EDSimulationEngine;
import it.unitn.disi.simulator.core.IProcess;
import it.unitn.disi.simulator.core.IEventObserver;
import it.unitn.disi.simulator.measure.IMetricAccumulator;
import it.unitn.disi.simulator.measure.INodeMetric;
import it.unitn.disi.utils.DenseIDMapper;
import it.unitn.disi.utils.collections.Pair;
import it.unitn.disi.utils.logging.IProgressTracker;

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

	private CloudTCE fLast;

	private IProcess[] fProcesses;

	private int[] fNodeMap;

	private int[] fContiguousMap;

	private HashMap<Integer, List<INodeMetric<Double>>> fMap = new HashMap<Integer, List<INodeMetric<Double>>>();

	public SimulationTaskBuilder(IndexedNeighborGraph graph, int[] ids, int root) {
		this(graph, ids, null, root);
	}

	public SimulationTaskBuilder(IndexedNeighborGraph graph, int[] ids,
			int[] nodeMap, int root) {
		fSims = new ArrayList<Pair<Integer, ? extends IEventObserver>>();
		fGraph = graph;
		fNodeMap = nodeMap;
		fIds = ids;
		fRoot = root;
	}

	public void createProcesses(double[] lIs, double[] dIs,
			YaoChurnConfigurator conf) {
		if (fNodeMap == null) {
			fProcesses = conf.createProcesses(lIs, dIs, dIs.length);
			return;
		}
		
		// Since some nodes got mapped into others, we need
		// to somehow make the id range contiguous again.
		// The next loop maps the IDs in fNodeMap (which are the 
		// actual processes to simulate) into a continuous ID range.
		DenseIDMapper mapper = new DenseIDMapper();
		for (int i = 0; i < fNodeMap.length; i++) {
			mapper.addMapping(fNodeMap[i]);
			System.err.println("Node " + i + " mapped to id "
					+ mapper.map(fNodeMap[i]));
		}

		double[] mappedLi = new double[mapper.size()];
		double[] mappedDi = new double[mapper.size()];

		// The LI/DI assignments are in the unmapped ID space,
		// so we map these as well.
		for (int i = 0; i < mappedDi.length; i++) {
			mappedLi[i] = lIs[mapper.reverseMap(i)];
			mappedDi[i] = dIs[mapper.reverseMap(i)];
		}

		// Finally, constructs the transitive map, which contains
		// fIdMap but using the now contiguous IDs. This maps from 
		// graph ID space => process ID space => contiguous ID space.
		fContiguousMap = new int[fGraph.size()];
		for (int i = 0; i < fContiguousMap.length; i++) {
			fContiguousMap[i] = mapper.map(fNodeMap[i]);
		}

		fProcesses = conf.createProcesses(mappedLi, mappedDi, mappedLi.length);
	}

	public SimulationTaskBuilder addConnectivitySimulation(int source,
			int[] fixedNodes, ActivationSampler sampler) {

		if (fNodeMap == null) {
			throw new UnsupportedOperationException(
					"Can't use node mapping with ComplexTCE.");
		}
		final CloudTCE tce = new CloudTCE(fGraph, source, fixedNodes,
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
				return tce.endToEndDelay(i);
			}
		});

		addMetric(source, new INodeMetric<Double>() {
			@Override
			public Object id() {
				return "rd";
			}

			@Override
			public Double getMetric(int i) {
				return tce.receiverDelay(i);
			}
		});

		return this;
	}

	public SimulationTaskBuilder addMultiConnectivitySimulation(int source,
			final int repeats, IMetricAccumulator<Double> ed,
			IMetricAccumulator<Double> rd, IProgressTracker tracker) {
		final MultiTCE multi = new MultiTCE(fGraph, source, repeats, ed, rd,
				tracker, fContiguousMap);
		addSim(multi);
		addMetric(source, ed);
		addMetric(source, rd);

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

		// Creates engine with zero permits so it arrests when all binding
		// observers are unbound.
		EDSimulationEngine engine = new EDSimulationEngine(fProcesses, burnIn,
				0);
		engine.setEventObservers(fSims);

		return new SimulationTask(fGraph.size(), engine, null, metrics);
	}
}
