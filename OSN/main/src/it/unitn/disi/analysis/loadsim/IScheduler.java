package it.unitn.disi.analysis.loadsim;

import java.util.List;
import java.util.NoSuchElementException;

public interface IScheduler {
	
	public static final String ROOT = "root";
	public static final String RANDOM = "random";
	public static final String PARENT = "parent";
	
	/**
	 * @param round
	 *            the round for which the schedule is to return experiments.
	 * @return a list of experiments scheduled to run at the supplied round.
	 * 
	 * @throws NoSuchElementException
	 *             if {@link #isOver()} returns <code>true</code>.
	 */
	public List<UnitExperiment> atTime(int round);

	/**
	 * @return <code>true</code> if the experiment is over, or
	 *         <code>false</code> otherwise.
	 */
	public boolean isOver();

	/**
	 * This method should be called by clients to inform the scheduler that a
	 * given experiment (previously returned by {@link #atTime(int)} is over.
	 * Note that the method should be called <b>before</b> the next call to
	 * {@link #atTime(int)}. Failure to comply to the contract may result in
	 * unspecified behavior from the scheduler implementation.
	 * 
	 * @param experiment
	 *            an experiment that has finished running.
	 * @return for convenience, returns <code>true</code> if the schedule is
	 *         over, or <code>false</code> otherwise.
	 */
	public boolean experimentDone(UnitExperiment experiment);
}
