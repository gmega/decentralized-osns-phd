package it.unitn.disi.churn.intersync;

import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import it.unitn.disi.churn.RenewalProcess;
import it.unitn.disi.churn.StateAccountant;
import it.unitn.disi.utils.streams.PrefixedWriter;
import it.unitn.disi.utils.tabular.TableWriter;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.PriorityQueue;
import org.junit.internal.matchers.IsCollectionContaining;

import peersim.util.IncrementalStats;

public class SyncExperiment implements Runnable {

	/** The two RenewalProcesses we are going to synchronize. */
	private final RenewalProcess fP1;
	private final RenewalProcess fP2;

	/** Current simulation time. */
	private volatile double fTime;

	private volatile int fSyncs;

	private boolean fDone;

	private final IncrementalStats fStats;

	private TDoubleArrayList fPendingUps;

	private final TableWriter fWriter = new TableWriter(new PrintWriter(
			new PrefixedWriter("SYNC:", new OutputStreamWriter(System.out))),
			"started", "finished", "duration");

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
		fStats = stats;
		fPendingUps = new TDoubleArrayList();
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

		// We saw a login event for P1.
		if (p == fP1 && p.isUp()) {
			fPendingUps.add(fTime);
		}

		// P1 and P2 are synchronized.
		if (fP1.isUp() && fP2.isUp()) {
			for (int i = 0; i < fPendingUps.size(); i++) {
				double started = fPendingUps.get(i);
				double duration = fTime - started;
				fWriter.set("started", started);
				fWriter.set("finished", fTime);
				fWriter.set("duration", duration);
				fWriter.emmitRow();
				fStats.add(duration);
			}
			fPendingUps.clear();
		}
		
		fSyncs--;

		if (fSyncs == 0) {
			fDone = true;
		}
	}

	public double time() {
		return fTime;
	}

}
