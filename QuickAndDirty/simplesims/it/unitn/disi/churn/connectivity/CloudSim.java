package it.unitn.disi.churn.connectivity;

import it.unitn.disi.simulator.core.Binding;
import it.unitn.disi.simulator.core.EDSimulationEngine;
import it.unitn.disi.simulator.core.INetwork;
import it.unitn.disi.simulator.core.IProcess;
import it.unitn.disi.simulator.core.IEventObserver;
import it.unitn.disi.simulator.core.Schedulable;
import it.unitn.disi.simulator.core.ISimulationEngine;
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
@Binding
public class CloudSim implements IEventObserver {

	private final int fSource;

	private double[] fTimes;

	private int fReached;

	public CloudSim(int source, int size) {
		fSource = source;
		fTimes = new double[size];
		Arrays.fill(fTimes, Double.MAX_VALUE);
	}

	@Override
	public void eventPerformed(ISimulationEngine state, Schedulable schedulable,
			double nextShift) {

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

}
