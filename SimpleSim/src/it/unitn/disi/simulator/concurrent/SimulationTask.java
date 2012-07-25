package it.unitn.disi.simulator.concurrent;

import it.unitn.disi.simulator.core.EDSimulationEngine;
import it.unitn.disi.simulator.measure.INodeMetric;
import it.unitn.disi.simulator.random.SimulationTaskException;
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.collections.Pair;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class SimulationTask implements Callable<SimulationTask> {

	private final Pair<Integer, List<? extends INodeMetric<? extends Object>>>[] fMetrics;

	private final Object fId;

	private final int[] fSources;

	private final EDSimulationEngine fEngine;

	private final Map<String, ? extends Object> fParameters;

	/**
	 * Constructs a new {@link SimulationTask}.
	 * 
	 * @param engine
	 *            the {@link EDSimulationEngine} to run.
	 * @param metrics
	 *            an array of {@link INodeMetric} lists, one per source.
	 */
	public SimulationTask(
			Object id,
			EDSimulationEngine engine,
			Map<String, ? extends Object> parameters,
			Pair<Integer, List<? extends INodeMetric<? extends Object>>>[] metrics) {
		fMetrics = metrics;
		fEngine = engine;
		fId = id;
		fParameters = parameters;

		fSources = new int[fMetrics.length];
		for (int i = 0; i < metrics.length; i++) {
			fSources[i] = fMetrics[i].a;
		}
	}

	@Override
	public SimulationTask call() throws SimulationTaskException {
		try {
			fEngine.run();
		} catch (Exception ex) {
			throw new SimulationTaskException(fParameters, fEngine, ex);
		}
		return this;
	}

	public EDSimulationEngine engine() {
		return fEngine;
	}

	public int[] sources() {
		return fSources;
	}

	public List<? extends INodeMetric<?>> metric(int source) {
		return fMetrics[MiscUtils.indexOf(fSources, source)].b;
	}

	public Object id() {
		return fId;
	}
}
