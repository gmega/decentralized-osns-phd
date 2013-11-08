package it.unitn.disi.churn.connectivity;

import it.unitn.disi.churn.connectivity.tce.ActivationSampler;
import it.unitn.disi.churn.connectivity.tce.CloudSim;
import it.unitn.disi.churn.connectivity.tce.CloudTCE;
import it.unitn.disi.churn.connectivity.tce.ComponentTracker;
import it.unitn.disi.churn.connectivity.tce.MultiTCE;
import it.unitn.disi.churn.connectivity.tce.NodeMappedTCE;
import it.unitn.disi.churn.connectivity.tce.SimpleRDTCE;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.simulator.churnmodel.yao.YaoChurnConfigurator;
import it.unitn.disi.simulator.concurrent.SimulationTask;
import it.unitn.disi.simulator.core.EngineBuilder;
import it.unitn.disi.simulator.core.IEventObserver;
import it.unitn.disi.simulator.core.IProcess;
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

	private final EngineBuilder fBuilder;

	private final IndexedNeighborGraph fGraph;

	private final int fRoot;

	private CloudTCE fLast;

	private int[] fContiguousMap;

	private HashMap<Integer, List<INodeMetric<Double>>> fMap = new HashMap<Integer, List<INodeMetric<Double>>>();

	public SimulationTaskBuilder(IndexedNeighborGraph graph, int root,
			double[] li, double[] di, YaoChurnConfigurator conf) {
		this(graph, root, li, di, conf, null);
	}

	public SimulationTaskBuilder(IndexedNeighborGraph graph, int root,
			double[] li, double[] di, YaoChurnConfigurator conf, int[] nodeMap) {
		fBuilder = new EngineBuilder();
		fGraph = graph;
		fRoot = root;

		fBuilder.addProcess(nodeMap == null ? conf.createProcesses(li, di,
				graph.size()) : createLinkedProcesses(li, di, conf, nodeMap));
	}

	public SimulationTaskBuilder addConnectivitySimulation(int source,
			String ed, String rd) {
		SimpleRDTCE tce = fContiguousMap == null ? new SimpleRDTCE(fGraph,
				source) : new NodeMappedTCE(fGraph, source, fContiguousMap);
		addTCEMetrics(source, tce, ed, rd);
		addSim(tce, true);
		return this;
	}

	public SimulationTaskBuilder addConnectivitySimulation(int source,
			int[] fixedNodes, ActivationSampler sampler, String ed, String rd) {
		// For lack of use cases, CloudTCE does not support node mapping.
		if (fContiguousMap != null) {
			throw new UnsupportedOperationException(
					"Cannot use node maps with cloud simulations.");
		}

		CloudTCE tce = new CloudTCE(fGraph, source, fixedNodes, sampler);
		fLast = tce;
		addTCEMetrics(source, tce, ed, rd);
		addSim(fLast, true);
		return this;
	}

	public SimulationTaskBuilder addMultiConnectivitySimulation(int source,
			final int repeats, IMetricAccumulator<Double> ed,
			IMetricAccumulator<Double> rd, IProgressTracker tracker) {
		final MultiTCE multi = new MultiTCE(fGraph, source, repeats, ed, rd,
				tracker, fContiguousMap);
		addSim(multi, true);
		addMetric(source, ed);
		addMetric(source, rd);
		return this;
	}

	public void addCloudSimulation(int source) {
		final CloudSim sim = new CloudSim(source, fGraph.size());
		addSim(sim, true);
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

	public void andComponentTracker(int source) {
		if (fLast == null) {
			throw new IllegalStateException();
		}
		addSim(new ComponentTracker(fLast, fGraph, System.out, fRoot, source,
				fLast.source()), false);
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
		
		fBuilder.setBurnin(burnIn);

		return new SimulationTask(fGraph.size(), fBuilder.engine(), null,
				metrics);
	}

	private void addTCEMetrics(int source, final SimpleRDTCE tce,
			final String edId, final String rdId) {

		addMetric(source, new INodeMetric<Double>() {
			@Override
			public Object id() {
				return edId;
			}

			@Override
			public Double getMetric(int i) {
				return tce.endToEndDelay(i);
			}
		});

		addMetric(source, new INodeMetric<Double>() {
			@Override
			public Object id() {
				return rdId;
			}

			@Override
			public Double getMetric(int i) {
				return tce.receiverDelay(i);
			}
		});

	}

	private IProcess[] createLinkedProcesses(double[] lIs, double[] dIs,
			YaoChurnConfigurator conf, int[] nodeMap) {
		// Since some nodes got mapped into others, we need
		// to somehow make the id range contiguous again.
		// The next loop maps the IDs in fNodeMap (which are the
		// actual processes to simulate) into a continuous ID range.
		DenseIDMapper mapper = new DenseIDMapper();
		for (int i = 0; i < nodeMap.length; i++) {
			mapper.addMapping(nodeMap[i]);
			System.err.println("Node " + i + " mapped to id "
					+ mapper.map(nodeMap[i]));
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
			fContiguousMap[i] = mapper.map(nodeMap[i]);
		}

		return conf.createProcesses(mappedLi, mappedDi, mappedLi.length);
	}

	private void addSim(IEventObserver observer, boolean binding) {
		fBuilder.addObserver(observer, IProcess.PROCESS_SCHEDULABLE_TYPE,
				binding, true);
	}

	private void addMetric(int source, INodeMetric<Double> metric) {
		List<INodeMetric<Double>> list = fMap.get(source);
		if (list == null) {
			list = new ArrayList<INodeMetric<Double>>();
			fMap.put(source, list);
		}

		list.add(metric);
	}
}
