package it.unitn.disi.newscasting.experiments.configurators;

import it.unitn.disi.newscasting.experiments.ExperimentStatisticsManager;
import it.unitn.disi.unitsim.cd.ICDExperimentObserver;
import it.unitn.disi.unitsim.cd.ICDUnitExperiment;
import peersim.config.Attribute;
import peersim.config.AutoConfig;

@AutoConfig
public class StatisticsPrinter implements ICDExperimentObserver {

	@Attribute("load_per_cycle")
	private boolean fLoadPerCycle;
	
	@Attribute("load")
	private boolean fLoad;

	@Attribute("latency")
	private boolean fLatency;

	private final ExperimentStatisticsManager fManager = ExperimentStatisticsManager
			.getInstance();

	@Override
	public void experimentStart(ICDUnitExperiment experiment) {
	}

	@Override
	public void experimentCycled(ICDUnitExperiment experiment) {
		if (fLoadPerCycle) {
			fManager.printLoadStatistics(System.out);
		}
	}

	@Override
	public void experimentEnd(ICDUnitExperiment experiment) {
		if (fLoad) {
			fManager.printLoadStatistics(System.out);
		}
		if (fLatency) {
			fManager.printLatencyStatistics(System.out);
		}
	}

}
