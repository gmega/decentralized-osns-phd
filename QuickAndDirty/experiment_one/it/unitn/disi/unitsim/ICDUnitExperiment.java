package it.unitn.disi.unitsim;

/**
 * Interface for a cycle-driven unit experiment.
 * 
 * @author giuliano
 */
public interface ICDUnitExperiment {

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
	 * @return whether or not this unit experiment is over. Note that this
	 *         doesn't mean that there won't be any more cycles for this
	 *         experiment -- the scheduler is the one deciding when to stop
	 *         cycling this experiment.
	 */
	public boolean isOver();

	/**
	 * Called after the last cycle of the current experiment. Experiments should
	 * release their resources here.
	 */
	public void done();
}
