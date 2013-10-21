package it.unitn.disi.simulator.util;

import it.unitn.disi.simulator.core.ISimulationEngine;
import it.unitn.disi.simulator.core.Schedulable;
import it.unitn.disi.utils.logging.IProgressTracker;
import it.unitn.disi.utils.logging.Progress;

/**
 * Utility class for displaying progress information for fixed-length
 * simulations.
 * 
 * @author giuliano
 */
public class TimedProgressTracker extends Schedulable {

	private static final long serialVersionUID = 1L;

	private int fType;

	private double fNextTick;

	private double fTickLength;

	private IProgressTracker fTracker;

	public TimedProgressTracker(String title, double duration, int type) {
		fTracker = Progress.newTracker(title, 100);
		fTickLength = duration / 100;
		fType = type;
	}

	public TimedProgressTracker(double duration, int type) {
		this("simulation task", duration, type);
	}

	@Override
	public boolean isExpired() {
		return false;
	}

	@Override
	public void scheduled(ISimulationEngine state) {
		if (fNextTick == 0) {
			fTracker.startTask();
		}
		fTracker.tick();
		fNextTick += fTickLength;
	}

	@Override
	public double time() {
		return fNextTick;
	}

	@Override
	public int type() {
		return fType;
	}

}
