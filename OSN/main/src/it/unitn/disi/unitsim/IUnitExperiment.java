package it.unitn.disi.unitsim;

public interface IUnitExperiment {

	public static final String ID = "id";

	/**
	 * @return the ID for this unit experiment.
	 */
	public Object getId();

	/**
	 * Called right after the unit experiment is scheduled, and before its first
	 * cycle.
	 */
	public void initialize();

	/**
	 * Called after the last cycle of the current experiment. Experiments should
	 * release their resources here.
	 */
	public void done();

	/**
	 * @return the simulation time in which this experiment has started.
	 */
	public long startTime();

	/**
	 * @return whether this experiment has timed out or not.
	 */
	public boolean isTimedOut();
}