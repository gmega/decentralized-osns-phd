package it.unitn.disi.churn.diffusion.cloud;

import it.unitn.disi.churn.diffusion.DisseminationServiceImpl.IBroadcastObserver;
import it.unitn.disi.churn.diffusion.HFloodMMsg;
import it.unitn.disi.simulator.core.IClockData;
import it.unitn.disi.simulator.core.ISimulationEngine;
import it.unitn.disi.simulator.measure.INodeMetric;

public class SessionStatistics implements IBroadcastObserver,
		ITimeWindowTracker {

	protected final Object fId;

	private double fLastSession = -1;

	private double fAccruedTime;

	public SessionStatistics(Object id) {
		fId = id;
	}

	public void startTrackingSession(IClockData clock) {
		if (isCounting()) {
			throw new IllegalStateException(
					"Nested sessions are not supported.");
		}
		fLastSession = clock.rawTime();
	}

	public void stopTrackingSession(IClockData clock) {
		if (!isCounting()) {
			throw new IllegalStateException(
					"Can't stop a tracking session when non was started.");
		}

		fAccruedTime += (clock.rawTime() - fLastSession);
		fLastSession = -1;
	}

	public double lastSessionStart() {
		return fLastSession;
	}

	public boolean isCounting() {
		return fLastSession != -1;
	}

	public INodeMetric<Double> accruedTime() {
		return new INodeMetric<Double>() {

			@Override
			public Object id() {
				return fId + ".accrued";
			}

			@Override
			public Double getMetric(int i) {
				return fAccruedTime;
			}

		};
	}

	@Override
	public void broadcastStarted(HFloodMMsg message, ISimulationEngine engine) {
		this.startTrackingSession(engine.clock());
	}

	@Override
	public void broadcastDone(HFloodMMsg message, ISimulationEngine engine) {
		this.stopTrackingSession(engine.clock());
	}
}
