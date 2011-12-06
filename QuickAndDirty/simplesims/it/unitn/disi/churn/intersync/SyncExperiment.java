package it.unitn.disi.churn.intersync;

import it.unitn.disi.churn.RenewalProcess;

import java.util.PriorityQueue;

import peersim.util.IncrementalStats;

public class SyncExperiment implements Runnable {

	static final byte S00 = 0x00;
	static final byte S01 = 0x01;
	static final byte S10 = 0x02;
	static final byte S11 = 0x03;

	/** The two RenewalProcesses we are going to synchronize. */
	private final RenewalProcess fP1;
	private final RenewalProcess fP2;

	/** Keeps track of intersync time stats. */
	private final IncrementalStats fLatency;

	/** Current simulation time. */
	private volatile double fTime;

	/** Burn-in time. */
	private volatile double fBurnin;

	/** Start of the last desync epoch. */
	private volatile double fLast = -1.0;

	/** Current synchronization state. */
	private volatile byte fCurrent;

	private volatile int fZeroes = 0;

	private volatile int fSyncs;

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
		fBurnin = burnin;
		fSyncs = syncs;
		fCurrent = currentState();
		fLatency = stats;
		fVerbose = verbose;
		fEid = eid;
	}

	public void run() {
		PriorityQueue<RenewalProcess> queue = new PriorityQueue<RenewalProcess>();
		fP1.next();
		fP2.next();

		updateSync();

		queue.add(fP1);
		queue.add(fP2);

		while (!queue.isEmpty() && !done()) {
			RenewalProcess p = queue.remove();
			fTime = p.nextSwitch();
			p.next();
			queue.add(p);
			updateSync();
		}
	}

	public IncrementalStats accounting() {
		return fLatency;
	}

	public long zeroes() {
		return fZeroes;
	}

	private void updateSync() {
		byte state = currentState();
		if (state == fCurrent) {
			return;
		}

		// New state.
		switch (state) {

		case S10:
			// We are not in a desync epoch.
			// Start counting.
			if (noDesyncEpoch()) {
				startDesyncEpoch();
			}
			break;

		case S11:
			endDesyncEpoch();
			break;
		}

		fCurrent = state;
	}

	private boolean noDesyncEpoch() {
		return fLast < 0;
	}

	private void startDesyncEpoch() {
		fLast = fTime;
	}

	private void endDesyncEpoch() {
		double duration = 0.0D;
		if (!noDesyncEpoch()) {
			duration = fTime - fLast;
		}
		account(duration);
		fLast = -1;
	}

	private void account(double duration, IncrementalStats... stats) {
		// If the start time for the current epoch is behind the
		// burn-in mark, don't account.
		if (fLast < fBurnin) {
			if (fVerbose) {
				System.out.println("SB: " + fEid + " " + duration);
			}
			return;
		}

		if (fVerbose) {
			System.out.println("SA: " + fEid + " " + duration);
		}

		if (duration != 0.0D) {
			fLatency.add(duration);
		} else {
			fZeroes++;
		}
		fSyncs--;
	}

	private boolean done() {
		return fSyncs <= 0;
	}

	private byte currentState() {
		byte state = 0;
		if (fP1.isUp()) {
			state |= 2;
		}

		if (fP2.isUp()) {
			state |= 1;
		}

		return state;
	}

}
