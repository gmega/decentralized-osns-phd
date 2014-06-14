package it.unitn.disi.simulator.measure;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

@SuppressWarnings("rawtypes")
public class MetricsCollector implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private final HashMap<Object, IMetricAccumulator> fMetrics = new HashMap<Object, IMetricAccumulator>();

	public MetricsCollector(IMetricAccumulator... accumulators) {
		for (IMetricAccumulator accumulator : accumulators) {
			addAccumulator(accumulator);
		}
	}

	public void addAccumulator(IMetricAccumulator accumulator) {
		fMetrics.put(accumulator.id(), accumulator);
	}

	public void add(List<? extends INodeMetric<? extends Object>> metrics) {
		for (INodeMetric<?> metric : metrics) {
			this.add(metric);
		}
	}

	@SuppressWarnings("unchecked")
	public synchronized void add(INodeMetric metric) {
		IMetricAccumulator accum = fMetrics.get(metric.id());
		if (accum != null) {
			accum.add(metric);
		}
	}

	public IMetricAccumulator getMetric(Object id) {
		return fMetrics.get(id);
	}
}
