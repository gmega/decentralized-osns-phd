package it.unitn.disi.churn.intersync;

import it.unitn.disi.churn.StateAccountant;
import it.unitn.disi.churn.simulator.IProcess;
import it.unitn.disi.churn.simulator.Schedulable;
import it.unitn.disi.churn.simulator.SimpleEDSim;
import it.unitn.disi.churn.simulator.IEventObserver;
import it.unitn.disi.churn.simulator.IValueObserver;
import it.unitn.disi.churn.simulator.RenewalProcess;

public class BurninSyncEstimator implements IEventObserver {

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
	public void simulationStarted(SimpleEDSim p) {
	}

	@Override
	public void stateShifted(SimpleEDSim parent, double time,
			Schedulable schedulable) {

		IProcess process = (IProcess) schedulable;

		if (time < fBurnin) {
			return;
		}

		// Login event for P1.
		if (parent.process(0) == process && process.isUp()) {
			if (fWaitingLogin) {
				fWaitingLogin = false;
				fWaitingSync = true;
				fWaitSync.enterState(time);
			}
		}

		// P1 and P2 are synchronized.
		if (fWaitingSync) {
			if (parent.process(0).isUp() && parent.process(1).isUp()) {
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
