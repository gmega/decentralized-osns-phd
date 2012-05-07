package it.unitn.disi.churn.simulator;

import it.unitn.disi.utils.collections.Pair;

import java.util.List;
import java.util.PriorityQueue;

/**
 * Simple discrete event simulator tailored for churn simulations.
 * 
 * @author giuliano
 */
public class SimpleEDSim implements Runnable, INetwork {

	private final IProcess[] fProcesses;

	private final PriorityQueue<Schedulable> fQueue;

	private final IEventObserver[][] fSim;
	
	private final int fObservers;

	private double fTime = -1;

	private double fBurnin;

	// -------------------------------------------------------------------------

	public SimpleEDSim(IProcess[] processes,
			List<Pair<Integer, ? extends IEventObserver>> delegates,
			double burnin) {

		fProcesses = processes;
		fQueue = new PriorityQueue<Schedulable>();
		for (Schedulable process : processes) {
			fQueue.add(process);
		}

		fSim = new IEventObserver[maxType(delegates) + 1][];
		for (int i = 0; i < fSim.length; i++) {
			fSim[i] = new IEventObserver[count(delegates, i)];
			int k = 0;
			for (int j = 0; j < delegates.size(); j++) {
				Pair<Integer, ? extends IEventObserver> delegate = delegates
						.get(j);
				if (delegate.a == i) {
					fSim[i][k++] = delegate.b;
				}
			}
		}

		fBurnin = burnin;
		fObservers = delegates.size();
	}

	// -------------------------------------------------------------------------

	public void run() {
		for (int i = 0; i < fSim.length; i++) {
			for (int j = 0; j < fSim[i].length; j++) {
				fSim[i][j].simulationStarted(this);				
			}
		}

		int done = 0;
		while (!fQueue.isEmpty() && !(done == fObservers)) {
			Schedulable p = fQueue.remove();
			fTime = p.time();
			p.scheduled(fTime, this);
			if (!p.isExpired()) {
				fQueue.add(p);
			}

			// Only run the simulations if burn in time is over.
			if (fTime >= fBurnin) {
				done = notifyObservers(done, p);
			}
		}
	}

	// -------------------------------------------------------------------------

	public void schedule(Schedulable schedulable) {
		if (schedulable.time() <= fTime) {
			throw new IllegalStateException("Can't schedule "
					+ "event in the past.");
		}
		fQueue.add(schedulable);
	}

	// -------------------------------------------------------------------------

	public double currentTime() {
		return fTime;
	}

	// -------------------------------------------------------------------------

	public double postBurninTime() {
		return currentTime() - fBurnin;
	}

	// -------------------------------------------------------------------------

	private int notifyObservers(int done, Schedulable p) {
		for (IEventObserver sim : fSim[p.type()]) {
			if (!sim.isDone()) {
				sim.stateShifted(this, fTime - fBurnin, p);
				if (sim.isDone()) {
					done++;
				}
			}
		}
		return done;
	}

	// -------------------------------------------------------------------------

	private int count(List<Pair<Integer, ? extends IEventObserver>> delegates,
			int type) {
		int count = 0;
		for (Pair<Integer, ? extends IEventObserver> pair : delegates) {
			if (pair.a == type) {
				count++;
			}
		}
		return count;
	}

	// -------------------------------------------------------------------------

	private int maxType(List<Pair<Integer, ? extends IEventObserver>> delegates) {
		int mx = -1;
		for (Pair<Integer, ? extends IEventObserver> pair : delegates) {
			if (pair.a > mx) {
				mx = pair.a;
			}
		}
		
		return mx;
	}

	// -------------------------------------------------------------------------

	public int size() {
		return fProcesses.length;
	}

	// -------------------------------------------------------------------------

	/* (non-Javadoc)
	 * @see it.unitn.disi.churn.simulator.INetwork#process(int)
	 */
	@Override
	public IProcess process(int index) {
		return fProcesses[index];
	}
}
