package it.unitn.disi.churn.intersync;

import it.unitn.disi.churn.BaseChurnSim;
import it.unitn.disi.churn.IChurnSim;
import it.unitn.disi.churn.RenewalProcess;
import it.unitn.disi.churn.StateAccountant;
import it.unitn.disi.churn.RenewalProcess.State;

import peersim.util.IncrementalStats;

public class SyncExperiment implements IChurnSim {

	private StateAccountant fWaitSync;

	private boolean fWaitingLogin = true;

	private boolean fWaitingSync = false;

	private volatile int fSyncs;

	private boolean fDone;

	private double fBurnin;

	public SyncExperiment(double burnin, int syncs) {
		this(burnin, syncs, new IncrementalStats());
	}

	public SyncExperiment(double burnin, int syncs, IncrementalStats stats) {
		fSyncs = syncs;
		fBurnin = burnin;
	}

	@Override
	public void simulationStarted(BaseChurnSim p, Object cookie) {
		fWaitSync = new StateAccountant((IncrementalStats) cookie,
				new IncrementalStats());
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

	@Override
	public void printStats(Object stats) {
		IncrementalStats iStats = (IncrementalStats) stats;
		System.out.println("E:" + iStats.getSum() + " " + iStats.getN() + " "
				+ iStats.getAverage() + " " + (iStats.getAverage() * 3600));
	}

}
