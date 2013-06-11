package it.unitn.disi.simulator.measure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Simple, {@link HashMap}-based implementation of {@link INamedValueObserver}.
 * Since key sets are fixed, future implementations should use a perfect hashing
 * function.
 * 
 * @author giuliano
 */
public class HashingValueObserver implements INamedValueObserver {

	public static HashingValueObserver create(int networkSize, String... keys) {
		HashMap<String, double[]> metrics = new HashMap<String, double[]>();
		for (String key : keys) {
			metrics.put(key, new double[networkSize]);
		}

		return new HashingValueObserver(metrics);
	}

	private final HashMap<String, double[]> fMetrics;

	private HashingValueObserver(HashMap<String, double[]> metrics) {
		fMetrics = metrics;
	}

	@Override
	public void observe(String metric, int node, double value) {
		double[] metrics = fMetrics.get(metric);
		if (metrics == null) {
			throw new NoSuchElementException();
		}
		metrics[node] += value;
	}

	public INodeMetric<Double> metric(final String key) {
		return new INodeMetric<Double>() {

			@Override
			public Object id() {
				return key;
			}

			@Override
			public Double getMetric(int i) {
				return fMetrics.get(key)[i];
			}

		};
	}

	public List<INodeMetric<Double>> metricList() {
		ArrayList<INodeMetric<Double>> metrics = new ArrayList<INodeMetric<Double>>();
		for (String key : fMetrics.keySet()) {
			metrics.add(metric(key));
		}
		
		return metrics;
	}
}
