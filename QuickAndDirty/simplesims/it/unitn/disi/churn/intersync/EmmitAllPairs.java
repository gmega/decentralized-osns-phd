package it.unitn.disi.churn.intersync;

import gnu.trove.list.array.TDoubleArrayList;
import it.unitn.disi.churn.BaseChurnSim;
import it.unitn.disi.churn.IChurnSim;
import it.unitn.disi.churn.RenewalProcess;
import it.unitn.disi.churn.RenewalProcess.State;
import it.unitn.disi.utils.streams.PrefixedWriter;
import it.unitn.disi.utils.tabular.TableWriter;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import peersim.util.IncrementalStats;

public class EmmitAllPairs implements IChurnSim {

	private volatile int fSyncs;

	private boolean fDone;

	private IncrementalStats fStats;

	private TDoubleArrayList fPendingUps;

	private final TableWriter fWriter = new TableWriter(new PrintWriter(
			new PrefixedWriter("SYNC:", new OutputStreamWriter(System.out))),
			"started", "finished", "duration");

	public EmmitAllPairs(int syncs) {
		fSyncs = syncs;
		fPendingUps = new TDoubleArrayList();
	}

	@Override
	public void simulationStarted(BaseChurnSim p, Object stats) {
		fStats = (IncrementalStats) stats;
	}

	@Override
	public void stateShifted(BaseChurnSim p, double time,
			RenewalProcess process, State old, State nw) {

		// We saw a login event for P1.
		if (process.id() == 0 && nw == State.up) {
			fPendingUps.add(time);
		}

		// P1 and P2 are synchronized.
		if (p.process(0).isUp() && p.process(1).isUp()) {
			for (int i = 0; i < fPendingUps.size(); i++) {
				double started = fPendingUps.get(i);
				double duration = time - started;
				fWriter.set("started", started);
				fWriter.set("finished", time);
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
