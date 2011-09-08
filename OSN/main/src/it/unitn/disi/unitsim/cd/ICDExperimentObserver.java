package it.unitn.disi.unitsim.cd;

import it.unitn.disi.unitsim.IExperimentObserver;


public interface ICDExperimentObserver extends IExperimentObserver<ICDUnitExperiment>{

	/**
	 * Hook method called after every cycle for which the current experiment is
	 * running.
	 */
	public void experimentCycled(ICDUnitExperiment experiment);

}
