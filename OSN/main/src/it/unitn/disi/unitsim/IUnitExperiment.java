package it.unitn.disi.unitsim;

public interface IUnitExperiment {

	public static final String ID = "id";

	/**
	 * @return the ID for this unit experiment.
	 */
	public abstract Object getId();

	/**
	 * Called right after the unit experiment is scheduled, and before its first
	 * cycle.
	 */
	public abstract void initialize();

	/**
	 * Called after the last cycle of the current experiment. Experiments should
	 * release their resources here.
	 */
	public abstract void done();

}