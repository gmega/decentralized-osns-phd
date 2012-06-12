package it.unitn.disi.churn.connectivity;

import it.unitn.disi.simulator.core.EDSimulationEngine;
import it.unitn.disi.simulator.core.INetwork;
import it.unitn.disi.simulator.core.IProcess;
import it.unitn.disi.simulator.core.ISimulationObserver;
import it.unitn.disi.simulator.core.Schedulable;
import it.unitn.disi.simulator.core.SimulationState;
import it.unitn.disi.unitsim.experiments.TemporalConnectivityExperiment;

import java.util.Arrays;

/**
 * {@link CloudSim} is a simpler {@link TemporalConnectivityEstimator} which
 * assumes that there is a server separating source and destination. Designed to
 * be stacked with {@link TemporalConnectivityExperiment} over a single
 * {@link EDSimulationEngine}.
 * 
 * @author giuliano
 */
public class CloudSim implements ISimulationObserver {

	private final int fSource;

	private double[] fTimes;

	private int fReached;

	public CloudSim(int source) {
		fSource = source;
	}

	@Override
	public void simulationStarted(EDSimulationEngine parent) {
		fTimes = new double[parent.size()];
		Arrays.fill(fTimes, Double.MAX_VALUE);
	}

	@Override
	public void eventPerformed(SimulationState state, Schedulable schedulable) {

		INetwork parent = state.network();
		IProcess process = (IProcess) schedulable;
		double time = state.clock().time();

		// Not a login event, don't care.
		if (!process.isUp()) {
			return;
		}

		// Source process not reached yet.
		if (!isReached(fSource)) {
			// If login for source, register time.
			if (process.id() == fSource) {
				// Everyone that's online also gets instantly reached (loop
				// will also mark the source).
				for (int i = 0; i < fTimes.length; i++) {
					if (parent.process(i).isUp()) {
						reached(time, i);
					}
				}
			}

			// Returns.
			return;
		}

		int id = process.id();
		if (!isReached(id)) {
			reached(time, id);
		}
	}

	private boolean isReached(int id) {
		return fTimes[id] != Double.MAX_VALUE;
	}

	private void reached(double time, int i) {
		fTimes[i] = time;
		fReached++;
		if (fReached > fTimes.length) {
			throw new IllegalStateException();
		}
	}

	@Override
	public boolean isDone() {
		return fReached == fTimes.length;
	}

	public double reachTime(int i) {
		return fTimes[i] - fTimes[fSource];
	}

	@Override
	public boolean isBinding() {
		return true;
	}

}
