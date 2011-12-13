package it.unitn.disi.churn.intersync;

import it.unitn.disi.churn.RenewalProcess;
import it.unitn.disi.churn.StateAccountant;

import java.util.PriorityQueue;
import org.junit.internal.matchers.IsCollectionContaining;

import peersim.util.IncrementalStats;

public class SyncExperiment implements Runnable {

	/** The two RenewalProcesses we are going to synchronize. */
	private final RenewalProcess fP1;
	private final RenewalProcess fP2;

	/** Current simulation time. */
	private volatile double fTime;

	private StateAccountant fWaitSync;

	private boolean fWaitingLogin = true;

	private boolean fWaitingSync = false;

	private volatile int fSyncs;

	private boolean fDone;
	
	private double fIdle = -1;
	
	private double fIdleMax;

	private final boolean fVerbose;

	private final String fEid;

	public SyncExperiment(RenewalProcess p1, RenewalProcess p2, double burnin,
			String eid, int syncs, boolean verbose) {
		this(p1, p2, burnin, eid, syncs, new IncrementalStats(), verbose);
	}

	public SyncExperiment(RenewalProcess p1, RenewalProcess p2, double burnin,
			String eid, int syncs, IncrementalStats stats, boolean verbose) {
		fP1 = p1;
		fP2 = p2;
		fSyncs = syncs;
		fVerbose = verbose;
		fEid = eid;
		fIdleMax = burnin;
		fWaitSync = new StateAccountant(stats, new IncrementalStats());
	}

	public void run() {
		PriorityQueue<RenewalProcess> queue = new PriorityQueue<RenewalProcess>();
		fP1.next();
		fP2.next();
		queue.add(fP1);
		queue.add(fP2);

		while (!queue.isEmpty() && !fDone) {
			RenewalProcess p = queue.remove();
			fTime = p.nextSwitch();
			p.next();
			queue.add(p);
			stateShifted(p);
		}
	}

	private void stateShifted(RenewalProcess p) {
		
		if (fTime < fIdle) {
			return;
		}
		
		// We saw a login event for P1.
		if (fWaitingLogin) {
			if (p == fP1 && p.isUp()) {
				fWaitingLogin = false;
				fWaitingSync = true;
				fWaitSync.enterState(fTime);
			}
		}

		// P1 and P2 are synchronized.
		if (fWaitingSync) {
			if (fP1.isUp() && fP2.isUp()) {
				fWaitSync.exitState(fTime);
				fSyncs--;
				fWaitingLogin = true;
				fWaitingSync = false;
				// Begin idle period.
				fIdle = fTime + idlePeriod();
			}
		}

		if (fSyncs == 0) {
			fDone = true;
		}
	}
	
	private double idlePeriod() {
		return fIdleMax;
	}

	public double time() {
		return fTime;
	}

}
