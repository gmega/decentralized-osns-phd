package it.unitn.disi.simulator.core;

import it.unitn.disi.utils.collections.Pair;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Simple discrete event simulator tailored for churn simulations.
 * 
 * @author giuliano
 */
public class EDSimulationEngine implements Runnable, INetwork, IClockData,
		ISimulationEngine, Serializable {

	private static final long serialVersionUID = 2524387383691123240L;

	private final IProcess[] fProcesses;

	private final PriorityQueue<Schedulable> fQueue;

	private IEventObserver[][] fObservers;

	private Set<IEventObserver> fBindingObservers = new HashSet<IEventObserver>();

	private boolean fRunning;

	private volatile boolean fDone;

	private double fTime = 0.0;

	private double fBurnin;

	private int fLive;

	// -------------------------------------------------------------------------

	public EDSimulationEngine(IProcess[] processes, double burnin) {

		fProcesses = processes;
		fQueue = new PriorityQueue<Schedulable>();

		// Counts initially live processes.
		for (IProcess process : processes) {
			if (process.isUp()) {
				fLive++;
			}
			fQueue.add(process);
		}

		fBurnin = burnin;
	}

	// -------------------------------------------------------------------------

	public void setEventObservers(
			List<Pair<Integer, ? extends IEventObserver>> observers) {
		checkNotRunning();
		// Allocates event observer arrays.
		fObservers = new IEventObserver[maxType(observers) + 1][];
		for (int i = 0; i < fObservers.length; i++) {
			fObservers[i] = new IEventObserver[count(observers, i)];
			int k = 0;
			for (int j = 0; j < observers.size(); j++) {
				Pair<Integer, ? extends IEventObserver> delegate = observers
						.get(j);
				if (delegate.a == i) {
					fObservers[i][k++] = delegate.b;
					if (delegate.b.getClass().getAnnotation(Binding.class) != null) {
						fBindingObservers.add(delegate.b);
					}
				}
			}
		}

		if (fBindingObservers.size() == 0) {
			throw new IllegalArgumentException(
					"At least one binding observer needs to be installed.");
		}
	}

	// -------------------------------------------------------------------------

	private void checkNotRunning() throws IllegalStateException {
		if (fRunning) {
			throw new IllegalStateException("Simulation is already running.");
		}
	}

	// -------------------------------------------------------------------------

	/**
	 * Runs the simulation until either: <li>
	 * <ol>
	 * <li>there are no more {@link Schedulable}s to schedule;</li>
	 * <li>all <b>binding</b> {@link IEventObserver}s are <b>done</b>.</li>
	 * </ol>
	 * 
	 * @see IEventObserver
	 */
	public void run() {
		checkNotRunning();
		checkCanRun();

		fRunning = true;

		// Main simulation loop.
		while (!fDone) {
			if (!uncheckedStep()) {
				break;
			}
		}

		fRunning = false;

		synchronized (this) {
			notify();
		}
	}

	// -------------------------------------------------------------------------

	public int step(int events) {
		checkCanRun();
		fRunning = true;

		for (int i = 0; i < events; i++) {
			if (!uncheckedStep() || fDone) {
				return i;
			}
		}

		return events;
	}

	// -------------------------------------------------------------------------

	public void checkCanRun() throws IllegalStateException {
		if (!fRunning && fDone) {
			throw new IllegalStateException("Can't step a simulation that's"
					+ " already done.");
		}
	}

	// -------------------------------------------------------------------------

	private boolean uncheckedStep() {
		if (fQueue.isEmpty()) {
			return false;
		}

		Schedulable p = fQueue.remove();
		fTime = p.time();
		p.scheduled(this);
		updateProcessCount(p);

		// Only run the simulations if burn in time is over.
		if (!isBurningIn()) {
			notifyObservers(p, time());
		}

		// It's best to do this here, so as to allow observer code an
		// opportunity to expire Schedulables before they get rescheduled.
		if (!p.isExpired()) {
			schedule(p);
		}

		return true;
	}

	// -------------------------------------------------------------------------

	/**
	 * Schedules a {@link Schedulable} in the event queue.
	 * 
	 * @param schedulable
	 *            the {@link Schedulable} to be scheduled.
	 */
	public void schedule(Schedulable schedulable) {
		if (schedulable.time() < fTime) {
			throw new IllegalStateException("Can't schedule "
					+ "event in the past (" + fTime + " > "
					+ schedulable.time() + ")");
		}
		fQueue.add(schedulable);
	}

	// -------------------------------------------------------------------------

	/**
	 * This method should be called by all binding observers once they are done,
	 * or once they cease to be binding. Failure to properly call this method
	 * may cause the simulation to enter an infinite loop.
	 * 
	 * @param observer
	 *            the observer that is now done.
	 */
	public void unbound(IEventObserver observer) {
		fBindingObservers.remove(observer);
		if (fBindingObservers.size() == 0) {
			fDone = true;
		}
	}

	// -------------------------------------------------------------------------

	@Override
	public void stop() {
		fDone = true;
	}

	// -------------------------------------------------------------------------

	/**
	 * The pause method might be called from another thread to temporarily
	 * interrupt execution of this simulation engine.
	 */
	public void pause() {
		// Stops the engine cold.
		stop();

		// Waits for the work thread to finish.
		synchronized (this) {
			while (fRunning) {
				try {
					this.wait();
				} catch (InterruptedException ex) {
					// Ignore it.
				}
			}
		}

		// Restores fDone status to allow resuming execution.
		fDone = false;
	}

	// -------------------------------------------------------------------------

	public boolean isDone() {
		return fDone;
	}

	// -------------------------------------------------------------------------

	private void notifyObservers(Schedulable p, double time) {
		int type = p.type();
		if (type >= fObservers.length) {
			return;
		}

		/*
		 * Note that we only dispatch events to observers of the type that
		 * matches the schedulable type.
		 */
		for (IEventObserver sim : fObservers[type]) {
			if (!sim.isDone()) {
				sim.eventPerformed(this, p,
						p.isExpired() ? IEventObserver.EXPIRED : p.time());
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

	@Override
	public double version() {
		return fTime;
	}

	// -------------------------------------------------------------------------

	public int live() {
		return fLive;
	}

	// -------------------------------------------------------------------------
	// ISimulationState interface.
	// -------------------------------------------------------------------------

	@Override
	public INetwork network() {
		return this;
	}

	@Override
	public IClockData clock() {
		return this;
	}

	// -------------------------------------------------------------------------

	/**
	 * Convenience method for telling whether the simulator is during its burnin
	 * period or not.
	 * 
	 * @return <code>true</code> if the simulator is during burn-in, or
	 *         <code>false</code> otherwose.
	 */
	@Override
	public boolean isBurningIn() {
		return fTime < fBurnin;
	}

	// -------------------------------------------------------------------------

	/**
	 * @return the current raw simulation time, including any burn-in period.
	 */

	@Override
	public double rawTime() {
		return fTime;
	}

	// -------------------------------------------------------------------------

	/**
	 * @return the time after the burn-in period, or 0 if burn-in is not over
	 *         yet.
	 */
	@Override
	public double time() {
		return Math.max(0, rawTime() - fBurnin);
	}
	
	// -------------------------------------------------------------------------
	
	public ISimulationEngine engine() {
		return this;
	}

	// -------------------------------------------------------------------------
}
