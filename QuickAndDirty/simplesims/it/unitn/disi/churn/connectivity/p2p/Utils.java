package it.unitn.disi.churn.connectivity.p2p;

import java.util.List;

import it.unitn.disi.simulator.INetworkMetric;


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
