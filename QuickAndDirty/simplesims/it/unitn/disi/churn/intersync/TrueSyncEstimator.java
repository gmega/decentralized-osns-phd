package it.unitn.disi.churn.intersync;

import gnu.trove.list.array.TDoubleArrayList;
import it.unitn.disi.churn.BaseChurnSim;
import it.unitn.disi.churn.IChurnSim;
import it.unitn.disi.churn.IValueObserver;
import it.unitn.disi.churn.RenewalProcess;
import it.unitn.disi.churn.RenewalProcess.State;

/**
 * Very simple experiment for estimating the true average for the
 * the synchronization time of two nodes.
 * 
 * @author giuliano
 */
public class TrueSyncEstimator implements IChurnSim {

	private volatile int fSyncs;

	private boolean fDone;

	private IValueObserver fObserver;

	private TDoubleArrayList fPendingUps;

	private int fPId0;

	public TrueSyncEstimator(int syncs, IValueObserver observer) {
		fSyncs = syncs;
		fPendingUps = new TDoubleArrayList();
		fObserver = observer;
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
		if (p.process(0).isUp() && p.process(1).isUp()) {
			for (int i = 0; i < fPendingUps.size(); i++) {
				fObserver.observe(time - fPendingUps.get(i));
			}
			fPendingUps.clear();
			fSyncs--;
		}

		if (fSyncs == 0) {
			fDone = true;
		}
	}

	@Override
	public boolean isDone() {
		return fDone;
	}

}
