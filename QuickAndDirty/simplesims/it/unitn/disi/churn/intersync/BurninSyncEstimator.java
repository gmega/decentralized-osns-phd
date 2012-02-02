package it.unitn.disi.churn.intersync;

import it.unitn.disi.churn.BaseChurnSim;
import it.unitn.disi.churn.IChurnSim;
import it.unitn.disi.churn.IValueObserver;
import it.unitn.disi.churn.RenewalProcess;
import it.unitn.disi.churn.StateAccountant;
import it.unitn.disi.churn.RenewalProcess.State;

public class BurninSyncEstimator implements IChurnSim {

	private StateAccountant fWaitSync;

	private boolean fWaitingLogin = true;

	private boolean fWaitingSync = false;

	private volatile int fSyncs;

	private boolean fDone;

	private double fBurnin;

	public BurninSyncEstimator(double burnin, int syncs, IValueObserver stats) {
		fSyncs = syncs;
		fBurnin = burnin;
		fWaitSync = new StateAccountant(stats, IValueObserver.NULL_OBSERVER);
	}

	@Override
	public void simulationStarted(BaseChurnSim p) {
	}

	@Override
	public void stateShifted(BaseChurnSim p, double time,
			RenewalProcess process, State old, State nw) {

		if (time < fBurnin) {
			return;
		}

		// Login event for P1.
		if (p.process(0) == process && nw == State.up) {
			if (fWaitingLogin) {
				fWaitingLogin = false;
				fWaitingSync = true;
				fWaitSync.enterState(time);
			}
		}

		// P1 and P2 are synchronized.
		if (fWaitingSync) {
			if (p.process(0).isUp() && p.process(1).isUp()) {
				fWaitSync.exitState(time);
				fSyncs--;
				fWaitingLogin = true;
				fWaitingSync = false;
			}
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
