package it.unitn.disi.churn.connectivity;

import java.util.Arrays;

import it.unitn.disi.churn.BaseChurnSim;
import it.unitn.disi.churn.IChurnSim;
import it.unitn.disi.churn.RenewalProcess;
import it.unitn.disi.churn.RenewalProcess.State;
import it.unitn.disi.unitsim.experiments.TemporalConnectivityExperiment;

/**
 * {@link CloudSim} is a simpler {@link TemporalConnectivityEstimator} which
 * assumes that there is a server separating source and destination. Designed to
 * be stacked with {@link TemporalConnectivityExperiment} over a single
 * {@link BaseChurnSim}.
 * 
 * @author giuliano
 */
public class CloudSim implements IChurnSim {

	private final int fSource;

	private double[] fTimes;

	private int fReached;

	public CloudSim(int source) {
		fSource = source;
	}

	@Override
	public void simulationStarted(BaseChurnSim parent) {
		fTimes = new double[parent.size()];
		Arrays.fill(fTimes, Double.MAX_VALUE);
	}

	@Override
	public void stateShifted(BaseChurnSim parent, double time,
			RenewalProcess process, State old, State nw) {

		// Not a login event, don't care.
		if (nw != State.up) {
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
