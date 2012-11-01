package it.unitn.disi.churn;

import it.unitn.disi.simulator.core.IClockData;
import it.unitn.disi.simulator.core.IEventObserver;
import it.unitn.disi.simulator.core.INetwork;
import it.unitn.disi.simulator.core.ISimulationEngine;
import it.unitn.disi.simulator.core.Schedulable;
import it.unitn.disi.simulator.measure.IValueObserver;

/**
 * Helper class for estimating transition probabilities in a Markov chain.
 * 
 * @author giuliano
 */
public class TransitionEstimator implements IEventObserver {

	private final IValueObserver fTimeToHit;

	private final INetworkState fOrigin;

	private final INetworkState fTarget;

	private final SimpleStopWatch fStopWatch = new SimpleStopWatch();

	public TransitionEstimator(INetworkState origin, INetworkState target,
			IValueObserver observer) {
		fTimeToHit = observer;
		fOrigin = origin;
		fTarget = target;
	}

	@Override
	public void eventPerformed(ISimulationEngine engine,
			Schedulable schedulable, double nextShift) {

		INetwork network = engine.network();
		IClockData clock = engine.clock();

		if (!fStopWatch.isCounting()) {
			if (fOrigin.holds(network)) {
				fStopWatch.start(clock.time());
			}
		} else {
			if (fTarget.holds(network)) {
				fTimeToHit.observe(fStopWatch.stop(clock.time()), null);
			}
		}

	}

	@Override
	public boolean isDone() {
		return false;
	}

}
