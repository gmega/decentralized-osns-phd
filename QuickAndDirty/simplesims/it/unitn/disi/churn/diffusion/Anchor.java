package it.unitn.disi.churn.diffusion;

import java.util.ArrayList;

import it.unitn.disi.churn.diffusion.cloud.CloudAccessStatistics;
import it.unitn.disi.churn.diffusion.cloud.ICloud;
import it.unitn.disi.churn.diffusion.cloud.ITimeWindowTracker;
import it.unitn.disi.simulator.core.EDSimulationEngine;
import it.unitn.disi.simulator.core.ISimulationEngine;
import it.unitn.disi.simulator.core.Schedulable;

/**
 * {@link Anchor} stops the simulation cold at a given time instant. It also
 * captures the points in time at which the {@link EDSimulationEngine} burnin
 * period ends and the anchor is dropped, which can be used as measurement
 * sessions.
 * 
 * @author giuliano
 */
public class Anchor extends Schedulable {

	private static final long serialVersionUID = 1L;

	private final int fType;

	private final double fBurnin;

	private final double fAnchorTime;

	private boolean fAnchorArmed = false;

	private boolean fAnchorDropped = false;

	private ArrayList<ITimeWindowTracker> fObservers = new ArrayList<ITimeWindowTracker>();

	private CloudAccessStatistics fAll;
	
	private ICloud fCloud;

	public Anchor(int type, int size, double burnin, double anchorTime, ICloud cloud) {
		fType = type;
		fBurnin = burnin;
		fAnchorTime = anchorTime;

		fAll = new CloudAccessStatistics("cloud_all", size);
		addMeasurementSessionObserver(fAll);
		fCloud = cloud;
	}

	@Override
	public boolean isExpired() {
		return fAnchorDropped;
	}

	@Override
	public void scheduled(ISimulationEngine engine) {
		if (fAnchorArmed) {
			fAnchorDropped = true;
			System.err.println("DRP: " + engine.clock().rawTime());
			for (ITimeWindowTracker observer : fObservers) {
				observer.stopTrackingSession(engine.clock());
			}
			// Stops the engine cold.
			engine.stop(engine.stopPermits());
			return;
		}

		System.err.println("Anchor armed (" + engine.clock().rawTime() + ").");

		fCloud.addAccessListener(fAll);
		
		for (ITimeWindowTracker observer : fObservers) {
			observer.startTrackingSession(engine.clock());
		}
		fAnchorArmed = true;
	}

	@Override
	public double time() {
		return fAnchorArmed ? fAnchorTime : fBurnin;
	}

	@Override
	public int type() {
		return fType;
	}

	public void addMeasurementSessionObserver(ITimeWindowTracker observer) {
		fObservers.add(observer);
	}

	public CloudAccessStatistics statistics() {
		return fAll;
	}

}
