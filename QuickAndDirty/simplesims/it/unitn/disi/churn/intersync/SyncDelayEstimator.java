package it.unitn.disi.churn.intersync;

import it.unitn.disi.churn.INetworkState;
import it.unitn.disi.churn.StationaryEstimator;
import it.unitn.disi.churn.TransitionEstimator;
import it.unitn.disi.simulator.core.Binding;
import it.unitn.disi.simulator.core.IEventObserver;
import it.unitn.disi.simulator.core.INetwork;
import it.unitn.disi.simulator.core.ISimulationEngine;
import it.unitn.disi.simulator.core.Schedulable;
import it.unitn.disi.simulator.measure.IValueObserver;
import peersim.config.Attribute;

@Binding
public class SyncDelayEstimator implements IEventObserver {

	private static final INetworkState _00 = new SyncState(false, false);
	private static final INetworkState _01 = new SyncState(false, true);
	private static final INetworkState _10 = new SyncState(true, false);
	private static final INetworkState _11 = new SyncState(true, true);

	@Attribute("samples")
	private int fTargetSamples;

	private int[] fActualSamples;

	private TransitionEstimator[] fTransitions;

	private TransitionEstimator[] fStationary;

	private boolean fDone;

	public SyncDelayEstimator(IValueObserver[] transitions,
			IValueObserver[] stationary) {
		fActualSamples = new int[6];
		fTransitions = transitionTrackers(transitions);
		fStationary = stationaryTrackers(stationary);
	}

	private TransitionEstimator[] transitionTrackers(IValueObserver[] observers) {
		TransitionEstimator[] estimators = new TransitionEstimator[3];
		estimators[0] = new TransitionEstimator(_00, _11, new CountWrapper(0, observers[0]));
		estimators[1] = new TransitionEstimator(_01, _11, new CountWrapper(1, observers[1]));
		estimators[2] = new TransitionEstimator(_10, _11, new CountWrapper(2, observers[2]));
		return estimators;
	}
	
	private TransitionEstimator[] stationaryTrackers(IValueObserver[] observers) {
		TransitionEstimator[] estimators = new TransitionEstimator[4];
		estimators[0] = new StationaryEstimator(_00, new CountWrapper(3, observers[0]));
		estimators[1] = new StationaryEstimator(_01, new CountWrapper(4, observers[1]));
		estimators[2] = new StationaryEstimator(_10, new CountWrapper(5, observers[2]));
		estimators[3] = new StationaryEstimator(_11, new CountWrapper(6, observers[3]));
		return estimators;
	}


	@Override
	public void eventPerformed(ISimulationEngine engine,
			Schedulable schedulable, double nextShift) {
		updateTrackers(fTransitions, engine);
		updateTrackers(fStationary, engine);
		updateDoneState(engine);
	}

	private void updateDoneState(ISimulationEngine engine) {
		fDone = true;
		for (int i = 0; i < fActualSamples.length; i++) {
			if (fActualSamples[i] < fTargetSamples) {
				fDone = false;
			}
		}

		if (fDone) {
			engine.unbound(this);
		}
	}

	private void updateTrackers(TransitionEstimator[] estimators,
			ISimulationEngine engine) {
		for (TransitionEstimator estimator : estimators) {
			estimator.eventPerformed(engine, null, 0.0);
		}
	}

	@Override
	public boolean isDone() {
		return fDone;
	}

	private class CountWrapper implements IValueObserver {

		private final int fIndex;

		private final IValueObserver fDelegate;

		public CountWrapper(int index, IValueObserver delegate) {
			fIndex = index;
			fDelegate = delegate;
		}

		@Override
		public void observe(double value) {
			fActualSamples[fIndex]++;
			fDelegate.observe(value);
		}

	}

	private static class SyncState implements INetworkState {

		private final boolean fP0;

		private final boolean fP1;

		public SyncState(boolean p0, boolean p1) {
			fP0 = p0;
			fP1 = p1;
		}

		@Override
		public boolean holds(INetwork network) {
			return (network.process(0).isUp() == fP0)
					&& (network.process(1).isUp() == fP1);
		}

	}

}
