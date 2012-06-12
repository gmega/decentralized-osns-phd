package it.unitn.disi.churn.connectivity.p2p;

import it.unitn.disi.simulator.measure.INetworkMetric;

import java.util.List;


public class Utils {
	public static INetworkMetric lookup(List<? extends INetworkMetric> list,
			String string) {
		for (INetworkMetric metric : list) {
			if (metric.id().equals(string)) {
				return metric;
			}
		}
		return null;
	}

}
