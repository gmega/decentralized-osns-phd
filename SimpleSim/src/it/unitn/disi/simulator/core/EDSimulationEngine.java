package it.unitn.disi.simulator.core;

import it.unitn.disi.utils.collections.Pair;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Simple discrete event simulator tailored for churn simulations. This class is
 * <code>not</code> thread-safe, though it is intended to be used with two
 * threads in the following specific situation:
 * 
 * <ol>
 * <li>one thread runs the main simulation loop ({@link #run()})</li>
 * <li>another thread calls {@link #pause()} and {@link #resume()}.</li>
 * <ol>
 * 
 * This is mainly to enable checkpointing: the engine can be paused until its
 * state gets dumped to disk, and then resumed.
 * 
 * 
 * @author giuliano
 */
public class EDSimulationEngine implements Runnable, INetwork, IClockData,
		ISimulationEngine, Serializable {

	private static final long serialVersionUID = 2524387383691123240L;

	private final IProcess[] fProcesses;

	private final PriorityQueue<Schedulable> fQueue;

	private final double fBurnin;

	private final Set<IEventObserver> fBindingObservers;

	private volatile boolean fPaused = false;

	private boolean fRunning;

	private boolean fDone;

	private double fTime = 0.0;

	private IEventObserver[][] fObservers;

	private int fLive;

	private int fStopPermits;

	// -------------------------------------------------------------------------

	EDSimulationEngine(IProcess[] processes, Descriptor[] descriptors,
			int extraPermits, double burnin) {

		this(processes, burnin, extraPermits);
		setEventObservers(descriptors);
		setStopPermits(fBindingObservers.size() + extraPermits);
	}

	// -------------------------------------------------------------------------

	/**
	 * Convenience constructor. Same as:
	 * 
	 * EDSimulationEngine(processes, burnin, 1);
	 * 
	 * @deprecated this method is now deprecated. Use {@link EngineBuilder}
	 *             instead.
	 * 
	 * @see #EDSimulationEngine(IProcess[], double, int)
	 */
	@Deprecated
	public EDSimulationEngine(IProcess[] processes, double burnin) {
		this(processes, burnin, 1);
	}

	// -------------------------------------------------------------------------

	/**
	 * Constructs a new event-drive simulation engine.
	 * 
	 * @deprecated this method is deprecated. Used {@link EngineBuilder}
	 *             instead.
	 * 
	 * @param processes
	 *            the {@link IProcess}es that compose this system.
	 * @param burnin
	 *            the burn-in period before which observers will not be
	 *            notified.
	 * @param stopPermits
	 *            the number of <b>stop permits</b> of this engine. This
	 *            corresponds to the number of times that {@link #stop()} has to
	 *            be called before the simulation stops.
	 */
	@Deprecated
	public EDSimulationEngine(IProcess[] processes, double burnin,
			int stopPermits) {
		fProcesses = processes;
		fQueue = new PriorityQueue<Schedulable>();
		fStopPermits = stopPermits;

		// Counts initially live processes.
		for (IProcess process : processes) {
			if (process.isUp()) {
				fLive++;
			}
			fQueue.add(process);
		}
		fBindingObservers = new HashSet<IEventObserver>();
		fBurnin = burnin;
	}

	// -------------------------------------------------------------------------

	/**
	 * @deprecated use {@link EngineBuilder} instead.
	 * 
	 * @param observers
	 */
	@Deprecated
	public void setEventObservers(
			List<Pair<Integer, ? extends IEventObserver>> observers) {
		checkNotRunning();

		Descriptor[] descriptors = new Descriptor[observers.size()];
		int i = 0;
		for (Pair<Integer, ? extends IEventObserver> pair : observers) {
			boolean binding = pair.b.getClass().getAnnotation(Binding.class) != null;
			descriptors[i] = new Descriptor(pair.b, pair.a, binding, true);
			i++;
		}

		setEventObservers(descriptors);

	}

	// -------------------------------------------------------------------------

	private void setEventObservers(Descriptor... descriptors) {
		fObservers = new IEventObserver[maxType(descriptors) + 1][];
		for (int i = 0; i < descriptors.length; i++) {
			fObservers[i] = new IEventObserver[count(descriptors, i)];
			int k = 0;
			for (int j = 0; j < descriptors.length; j++) {
				Descriptor descriptor = descriptors[j];
				if (descriptor.type == i && descriptor.listening) {
					fObservers[i][k++] = descriptor.observer;
					if (descriptor.binding) {
						fBindingObservers.add(descriptor.observer);
					}
				}
			}
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
	 * <li>all <b>binding</b> {@link IEventObserver}s are <b>done</b>;</li>
	 * <li>{@link #stop()} is called and {@link #stopPermits()} is zero.</li>
	 * </ol>
	 * 
	 * @see IEventObserver
	 */
	public synchronized void run() {
		checkNotRunning();
		checkCanRun();

		// Main simulation loop.
		while (!fDone) {
			waitForClearance();
			fRunning = true;
			while (!fPaused && !fDone) {
				if (!uncheckedStep()) {
					fDone = true;
					break;
				}
			}

			/**
			 * JMM note: write to volatile guarantees that the state seen by the
			 * thread calling pause -- which needs to read the volatile and find
			 * a false before returning -- will be consistent with all changes
			 * made by the thread running the main simulation loop before it
			 * paused.
			 */
			fRunning = false;
		}

	}

	// -------------------------------------------------------------------------

	private void waitForClearance() {
		while (fPaused) {
			try {
				this.notify();
				this.wait();
			} catch (InterruptedException ex) {
				// Does nothing.
			}
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
		if (!fBindingObservers.remove(observer)) {
			throw new IllegalStateException(
					"Observer attempted to unbind twice.");
		}
		stop(1);
	}

	// -------------------------------------------------------------------------

	@Override
	public void stop() {
		stop(1);
	}

	// -------------------------------------------------------------------------

	public int stopPermits() {
		return fStopPermits;
	}

	// -------------------------------------------------------------------------

	public void setStopPermits(int permits) {
		fStopPermits = permits;
	}

	// -------------------------------------------------------------------------

	/**
	 * The pause method might be called from another thread to temporarily
	 * interrupt execution of this simulation engine. When this method returns,
	 * it is guaranteed that the main simulation loop thread will be paused, and
	 * no changes can be made to the state of the simulation engine.
	 */
	public void pause() {
		fPaused = true;
		synchronized (this) {
			while (fRunning) {
				try {
					// Waits for the simulation loop thread to enter
					// waitForClearance.
					this.wait();
				} catch (InterruptedException e) {
					// does nothing.
				}
			}
		}
	}

	// -------------------------------------------------------------------------

	/**
	 * The resume method is called by another thread to resume execution of the
	 * simulation engine.
	 */
	public void resume() {
		fPaused = false;
		synchronized (this) {
			this.notify();
		}
	}

	// -------------------------------------------------------------------------

	public boolean isDone() {
		return fDone;
	}

	// -------------------------------------------------------------------------

	private void checkCanRun() throws IllegalStateException {
		if (!fRunning && fDone) {
			throw new IllegalStateException("Can't step a simulation that's"
					+ " already done.");
		}

		if (fStopPermits == 0) {
			throw new IllegalStateException(
					"Can't step a simulation that has no permits.");
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

	private void stop(int drain) {
		fStopPermits = Math.max(fStopPermits - drain, 0);

		if (fStopPermits == 0) {
			fDone = true;
		}
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
		/**
		 * XXX I ran the microbenchmark from:
		 * 
		 * http://www.theeggeadventure.com/wikimedia/index.php/
		 * InstanceOf_Performance
		 * 
		 * on a 1.7 JVM under Linux 3.2.0-31-generic (Ubuntu), 64 bits. Found
		 * that instanceof performance, with the server VM, is almost as fast as
		 * a type comparison. So I keep instanceof for being more flexible.
		 */
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

	private int count(Descriptor[] descriptors, int type) {
		int count = 0;
		for (Descriptor descriptor : descriptors) {
			if (descriptor.type == type && descriptor.listening) {
				count++;
			}
		}
		return count;
	}

	// -------------------------------------------------------------------------

	private int maxType(Descriptor[] descriptors) {
		int mx = -1;
		for (Descriptor descriptor : descriptors) {
			if (descriptor.type > mx && descriptor.listening) {
				mx = descriptor.type;
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

	// -------------------------------------------------------------------------

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

	public static class Descriptor {

		public final boolean binding;
		public final boolean listening;
		public final int type;
		public final IEventObserver observer;

		public Descriptor(IEventObserver observer, int type, boolean binding,
				boolean listening) {
			this.binding = binding;
			this.type = type;
			this.observer = observer;
			this.listening = listening;
		}
	}
}
