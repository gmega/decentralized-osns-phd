package it.unitn.disi.unitsim.cd;

import it.unitn.disi.unitsim.IUnitExperiment;

/**
 * Interface for a cycle-driven unit experiment.
 * 
 * @author giuliano
 */
public interface ICDUnitExperiment extends IUnitExperiment {

	/**
	 * Cycles the current unit experiment.
	 * 
	 * @return whether or not this unit experiment is over. Note that this
	 *         doesn't mean that there won't be any more cycles for this
	 *         experiment -- the scheduler is the one deciding when to stop
	 *         cycling this experiment.
	 */
	public boolean cycled();
}
