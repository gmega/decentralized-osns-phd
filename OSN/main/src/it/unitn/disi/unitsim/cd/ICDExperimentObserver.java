package it.unitn.disi.unitsim.cd;

import it.unitn.disi.unitsim.IUnitExperiment;


public interface ICDExperimentObserver {

	/**
	 * Hook method called when an experiment is about to start.
	 */
	public void experimentStart(IUnitExperiment experiment);

	/**
	 * Hook method called after every cycle for which the current experiment is
	 * running.
	 */
	public void experimentCycled(ICDUnitExperiment experiment);

	/**
	 * Hook method called when an experiment finishes.
	 */
	public void experimentEnd(ICDUnitExperiment experiment);

}
