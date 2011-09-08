package it.unitn.disi.unitsim;

public interface IExperimentObserver {
	/**
	 * Hook method called when an experiment is about to start.
	 */
	public void experimentStart(IUnitExperiment experiment);

	/**
	 * Hook method called when an experiment finishes.
	 */
	public void experimentEnd(IUnitExperiment experiment);
}
