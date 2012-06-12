package it.unitn.disi.churn.intersync;

import it.unitn.disi.churn.StateAccountant;
import it.unitn.disi.simulator.core.EDSimulationEngine;
import it.unitn.disi.simulator.core.INetwork;
import it.unitn.disi.simulator.core.IProcess;
import it.unitn.disi.simulator.core.ISimulationObserver;
import it.unitn.disi.simulator.core.Schedulable;
import it.unitn.disi.simulator.measure.IValueObserver;

public class BurninSyncEstimator implements ISimulationObserver {

	private EDSimulationEngine fParent;
	
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
	public void simulationStarted(EDSimulationEngine p) {
		fParent = p;
	}

	@Override
	public void eventPerformed(INetwork parent, double time,
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
			fParent.unbound(this);
		}
	}

	@Override
	public boolean isDone() {
		return fDone;
	}

	@Override
	public boolean isBinding() {
		return true;
	}

}
