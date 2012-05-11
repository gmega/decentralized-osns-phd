package it.unitn.disi.churn.simulator;

import it.unitn.disi.utils.collections.Pair;

import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Simple discrete event simulator tailored for churn simulations.
 * 
 * @author giuliano
 */
public class SimpleEDSim implements Runnable, INetwork {

	private final IProcess[] fProcesses;

	private final PriorityQueue<Schedulable> fQueue;

	private final IEventObserver[][] fSim;

	private Set<IEventObserver> fBindingObservers = new HashSet<IEventObserver>();

	private boolean fDone;

	private double fTime = 0.0;

	private double fBurnin;

	private int fLive;

	// -------------------------------------------------------------------------

	public SimpleEDSim(IProcess[] processes,
			List<Pair<Integer, ? extends IEventObserver>> delegates,
			double burnin) {

		fProcesses = processes;
		fQueue = new PriorityQueue<Schedulable>();

		// Counts initially live processes.
		for (IProcess process : processes) {
			if (process.isUp()) {
				fLive++;
			}
			fQueue.add(process);
		}

		// Allocates event observer arrays.
		fSim = new IEventObserver[maxType(delegates) + 1][];
		for (int i = 0; i < fSim.length; i++) {
			fSim[i] = new IEventObserver[count(delegates, i)];
			int k = 0;
			for (int j = 0; j < delegates.size(); j++) {
				Pair<Integer, ? extends IEventObserver> delegate = delegates
						.get(j);
				if (delegate.a == i) {
					fSim[i][k++] = delegate.b;
					if (delegate.b.isBinding()) {
						fBindingObservers.add(delegate.b);
					}
				}
			}
		}

		fBurnin = burnin;
	}

	// -------------------------------------------------------------------------

	public void run() {

		for (int i = 0; i < fSim.length; i++) {
			for (int j = 0; j < fSim[i].length; j++) {
				fSim[i][j].simulationStarted(this);
			}
		}

		while (!fQueue.isEmpty() && !fDone) {
			Schedulable p = fQueue.remove();
			fTime = p.time();
			p.scheduled(fTime, this);
			
			updateProcessCount(p);
			
			// Only run the simulations if burn in time is over.
			if (fTime >= fBurnin) {
				notifyObservers(p);
			}
			
			if (!p.isExpired()) {
				fQueue.add(p);
			}
		}
	}

	// -------------------------------------------------------------------------
	
	public void schedule(Schedulable schedulable) {
		if (schedulable.time() < fTime) {
			throw new IllegalStateException("Can't schedule "
					+ "event in the past (" + fTime + " > "
					+ schedulable.time() + ")");
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

	private void notifyObservers(Schedulable p) {
		for (IEventObserver sim : fSim[p.type()]) {
			if (!sim.isDone()) {
				sim.stateShifted(this, fTime - fBurnin, p);
			}
		}
	}

	// -------------------------------------------------------------------------

	private void updateProcessCount(Schedulable p) {
		if (!(p instanceof IProcess)) {
			return;
		}

		IProcess proc = (IProcess) p;
		if (proc.isUp()) {
			fLive++;
		} else {
			fLive--;
		}
	}

	// -------------------------------------------------------------------------

	public void done(IEventObserver observer) {
		fBindingObservers.remove(observer);
		if (fBindingObservers.size() == 0) {
			fDone = true;
		}
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
	// INetwork interface.
	// -------------------------------------------------------------------------

	@Override
	public int size() {
		return fProcesses.length;
	}

	// -------------------------------------------------------------------------

	@Override
	public IProcess process(int index) {
		return fProcesses[index];
	}

	// -------------------------------------------------------------------------

	public int live() {
		return fLive;
	}

	// -------------------------------------------------------------------------

	@Override
	public double version() {
		return fTime;
	}
}
