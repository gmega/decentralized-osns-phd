package it.unitn.disi.churn.diffusion;

import it.unitn.disi.churn.diffusion.DisseminationServiceImpl.IBroadcastObserver;
import it.unitn.disi.simulator.core.INetwork;
import it.unitn.disi.simulator.core.ISimulationEngine;
import it.unitn.disi.simulator.measure.INodeMetric;

public class UptimeTracker implements IBroadcastObserver{

	private final String fId;
	
	private double[] fUptimes;

	private double[] fAccruedUptimes;
	
	public UptimeTracker(String id, int size) {
		fAccruedUptimes = new double[size];
		fUptimes = new double[size];
		fId = id;
	}
	
	
	public INodeMetric<Double> accruedUptime() { 
		return new INodeMetric<Double>() {

			@Override
			public Object id() {
				return fId + ".uptime";
			}

			@Override
			public Double getMetric(int i) {
				return fAccruedUptimes[i];
			}

		};
	}

	@Override
	public void broadcastStarted(HFloodMMsg message, ISimulationEngine engine) {
		INetwork network = engine.network();
		for (int i = 0; i < fUptimes.length; i++) {
			fUptimes[i] = network.process(i).uptime(engine.clock());
		}
	}

	@Override
	public void broadcastDone(HFloodMMsg message, ISimulationEngine engine) {
		INetwork network = engine.network();
		for (int i = 0; i < fUptimes.length; i++) {
			fAccruedUptimes[i] += (network.process(i).uptime(engine.clock()) - fUptimes[i]);
		}
	}

}
