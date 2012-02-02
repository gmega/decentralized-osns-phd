package it.unitn.disi.churn;

import it.unitn.disi.churn.RenewalProcess.State;

import java.util.List;
import java.util.PriorityQueue;

/**
 * Exceedingly simple discrete event simulator for churn models.
 * 
 * @author giuliano
 */
public class BaseChurnSim implements Runnable {

	private final RenewalProcess[] fProcesses;

	private final PriorityQueue<RenewalProcess> fQueue;

	private final IChurnSim[] fSim;

	private double fTime;

	private double fBurnin;

	public BaseChurnSim(RenewalProcess[] processes, List<IChurnSim> delegates,
			double burnin) {
		fProcesses = processes;
		fQueue = new PriorityQueue<RenewalProcess>();
		for (RenewalProcess process : processes) {
			fQueue.add(process);
		}
		fSim = delegates.toArray(new IChurnSim[delegates.size()]);
		fBurnin = burnin;
	}

	public void run() {
		for (int i = 0; i < fSim.length; i++) {
			fSim[i].simulationStarted(this);
		}

		int done = 0;

		while (!fQueue.isEmpty() && !(done == fSim.length)) {
			RenewalProcess p = fQueue.remove();
			fTime = p.nextSwitch();
			State old = p.state();
			p.next();
			fQueue.add(p);

			// Only run the sims if burnin time is over.
			if (fTime >= fBurnin) {
				done = runSims(done, p, old);
			}
		}
	}

	private int runSims(int done, RenewalProcess p, State old) {
		for (IChurnSim sim : fSim) {
			if (!sim.isDone()) {
				sim.stateShifted(this, fTime - fBurnin, p, old, p.state());
				if (sim.isDone()) {
					done++;
				}
			}
		}
		return done;
	}

	public RenewalProcess process(int index) {
		return fProcesses[index];
	}
}
