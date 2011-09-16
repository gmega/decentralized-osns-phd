package it.unitn.disi.unitsim.ed;

import it.unitn.disi.unitsim.IExperimentObserver;
import it.unitn.disi.unitsim.IUnitExperiment;

/**
 * Interface for event-driven unit experiments.
 * 
 * @author giuliano
 */
public interface IEDUnitExperiment extends IUnitExperiment {

	/**
	 * Interrupts the currently running experiment.
	 * 
	 * @throws IllegalStateException
	 *             if the experiment cannot be interrupted.
	 */
	public void interruptExperiment();

	/**
	 * Registers a listener to event-driven unit experiments.
	 * 
	 * @see IExperimentObserver
	 */
	public void addObserver(IExperimentObserver<IEDUnitExperiment> observer);
}
