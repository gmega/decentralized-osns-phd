package it.unitn.disi.churn.intersync;

import gnu.trove.list.array.TDoubleArrayList;
import it.unitn.disi.churn.BaseChurnSim;
import it.unitn.disi.churn.IChurnSim;
import it.unitn.disi.churn.IValueObserver;
import it.unitn.disi.churn.RenewalProcess;
import it.unitn.disi.churn.RenewalProcess.State;

/**
 * Very simple experiment for sampling the synchronization time of two nodes.
 * 
 * @author giuliano
 */
public class TrueSyncEstimator implements IChurnSim {

	private volatile int fSamples;

	private final IValueObserver fObserver;

	private final TDoubleArrayList fPendingUps;

	private final boolean fCloud;

	private volatile int fPId0;

	public TrueSyncEstimator(int samples, boolean cloud, IValueObserver observer) {
		fSamples = samples;
		fPendingUps = new TDoubleArrayList();
		fObserver = observer;
		fCloud = cloud;
	}

	@Override
	public void simulationStarted(BaseChurnSim p) {
		fPId0 = p.process(0).id();
	}

	@Override
	public void stateShifted(BaseChurnSim p, double time,
			RenewalProcess process, State old, State nw) {

		// We saw a login event for P1.
		if (process.id() == fPId0 && nw == State.up) {
			fPendingUps.add(time);
		}

		// P1 and P2 are synchronized.
		if (senderUp(p) && p.process(1).isUp()) {
			for (int i = 0; i < fPendingUps.size(); i++) {
				fObserver.observe(time - fPendingUps.get(i));
				fSamples--;
			}
			fPendingUps.resetQuick();
		}
	}

	private boolean senderUp(BaseChurnSim p) {
		return fCloud || p.process(0).isUp();
	}

	@Override
	public boolean isDone() {
		return fSamples <= 0;
	}

}
