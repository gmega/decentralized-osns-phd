package it.unitn.disi.churn.intersync;

import gnu.trove.list.array.TDoubleArrayList;
import it.unitn.disi.churn.BaseChurnSim;
import it.unitn.disi.churn.IChurnSim;
import it.unitn.disi.churn.RenewalProcess;
import it.unitn.disi.churn.RenewalProcess.State;

import peersim.util.IncrementalStats;

public class TrueSyncEstimator implements IChurnSim {

	private volatile int fSyncs;

	private boolean fDone;

	private IncrementalStats fStats;

	private TDoubleArrayList fPendingUps;
	
	private int pId0;

	public TrueSyncEstimator(int syncs) {
		fSyncs = syncs;
		fPendingUps = new TDoubleArrayList();
	}

	@Override
	public void simulationStarted(BaseChurnSim p, Object stats) {
		fStats = (IncrementalStats) stats;
		pId0 = p.process(0).id();
	}

	@Override
	public void stateShifted(BaseChurnSim p, double time,
			RenewalProcess process, State old, State nw) {

		// We saw a login event for P1.
		if (process.id() == pId0 && nw == State.up) {
			fPendingUps.add(time);
		}

		// P1 and P2 are synchronized.
		if (p.process(0).isUp() && p.process(1).isUp()) {
			for (int i = 0; i < fPendingUps.size(); i++) {
				fStats.add(time - fPendingUps.get(i));
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

	@Override
	public void printStats(Object stats) {
		IncrementalStats iStats = (IncrementalStats) stats;
		System.out.println("E:" + iStats.getSum() + " " + iStats.getN() + " "
				+ iStats.getAverage() + " " + (iStats.getAverage() * 3600));
	}

}
