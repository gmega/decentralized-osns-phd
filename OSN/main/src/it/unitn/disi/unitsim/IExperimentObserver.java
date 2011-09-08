package it.unitn.disi.unitsim;

public interface IExperimentObserver<T extends IUnitExperiment> {
	/**
	 * Hook method called when an experiment is about to start.
	 */
	public void experimentStart(T experiment);

	/**
	 * Hook method called when an experiment finishes.
	 */
	public void experimentEnd(T experiment);
}
