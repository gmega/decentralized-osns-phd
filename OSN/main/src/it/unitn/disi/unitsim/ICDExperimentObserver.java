package it.unitn.disi.unitsim;


public interface ICDExperimentObserver {

	/**
	 * Hook method called when an experiment is about to start.
	 */
	public void experimentStart(ICDUnitExperiment experiment);

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
