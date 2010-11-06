package it.unitn.disi.newscasting.experiments.configurators;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.Node;
import it.unitn.disi.newscasting.experiments.ExperimentStatisticsManager;
import it.unitn.disi.newscasting.experiments.IExperimentObserver;

@AutoConfig
public class StatisticsPrinter implements IExperimentObserver {

	@Attribute("load")
	private boolean fLoad;

	@Attribute("latency")
	private boolean fLatency;

	private final ExperimentStatisticsManager fManager = ExperimentStatisticsManager
			.getInstance();

	@Override
	public void experimentStart(Node root) {
	}

	@Override
	public void experimentCycled(Node root) {
		if (fLoad) {
			fManager.printLoadStatistics(System.out);
		}
	}

	@Override
	public void experimentEnd(Node root) {
		if (fLoad) {
			fManager.printLoadStatistics(System.out);
		}
		if (fLatency) {
			fManager.printLatencyStatistics(System.out);
		}
	}

}
