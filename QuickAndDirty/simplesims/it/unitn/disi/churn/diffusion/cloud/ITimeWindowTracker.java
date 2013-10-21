package it.unitn.disi.churn.diffusion.cloud;

import it.unitn.disi.simulator.core.IClockData;

/**
 * An {@link ITimeWindowTracker} is interested in pairs of events that delimit a
 * time window.
 * 
 * @author giuliano
 */
public interface ITimeWindowTracker {
	
	public void startTrackingSession(IClockData clock);
	
	public void stopTrackingSession(IClockData clock);
	
}
