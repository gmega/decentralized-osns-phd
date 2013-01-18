package it.unitn.disi.churn.diffusion.cloud;

import it.unitn.disi.churn.diffusion.DisseminationServiceImpl.IBroadcastObserver;
import it.unitn.disi.churn.diffusion.HFloodMMsg;
import it.unitn.disi.simulator.core.ISimulationEngine;
import it.unitn.disi.simulator.measure.INodeMetric;

public class SessionStatistics implements IBroadcastObserver {

	protected final Object fId;

	private double fLastSession = -1;

	private double fAccruedTime;

	public SessionStatistics(Object id) {
		fId = id;
	}

	public void startTrackingSession(double time) {
		if (isCounting()) {
			throw new IllegalStateException(
					"Nested sessions are not supported.");
		}
		fLastSession = time;
	}

	public void stopTrackingSession(double time) {
		if (!isCounting()) {
			throw new IllegalStateException(
					"Can't stop a tracking session when non was started.");
		}

		fAccruedTime += (time - fLastSession);
		fLastSession = -1;
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
		this.startTrackingSession(engine.clock().rawTime());
	}

	@Override
	public void broadcastDone(HFloodMMsg message, ISimulationEngine engine) {
		this.stopTrackingSession(engine.clock().rawTime());
	}
}
