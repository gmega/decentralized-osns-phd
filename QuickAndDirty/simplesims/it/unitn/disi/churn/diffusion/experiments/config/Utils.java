package it.unitn.disi.churn.diffusion.experiments.config;

import it.unitn.disi.simulator.measure.INodeMetric;

import java.util.List;

public class Utils {
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static <T> INodeMetric<T> lookup(
			List<? extends INodeMetric<? extends Object>> list, String string,
			Class<T> klass) {
		for (INodeMetric metric : list) {
			if (metric.id().equals(string)) {
				return metric;
			}
		}
		return null;
	}

}
