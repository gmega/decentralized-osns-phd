package it.unitn.disi.simulator.core;

import it.unitn.disi.simulator.core.EDSimulationEngine.Descriptor;
import it.unitn.disi.simulator.util.Anchor;
import it.unitn.disi.simulator.util.TimedProgressTracker;
import it.unitn.disi.utils.logging.IProgressTracker;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link EngineBuilder} allows clients to instantiate
 * {@link EDSimulationEngine}.
 * 
 * @author giuliano
 */
public class EngineBuilder {

	private static final int RESIZE_THRESHOLD = 1000;

	private List<Descriptor> fEventObservers;

	private List<ILifecycleObserver> fLifecycleObservers;

	private List<IProcess> fProcesses;

	private ArrayList<Schedulable> fPreschedulables;

	private int fExtraPermits;

	private int fBinding;

	private double fBurnin;

	private EDSimulationEngine fInstance;

	/**
	 * Creates a new {@link EngineBuilder}. A builder can only build a single
	 * engine.
	 */
	public EngineBuilder() {
		fEventObservers = new ArrayList<Descriptor>();
		fProcesses = new ArrayList<IProcess>();
		fLifecycleObservers = new ArrayList<ILifecycleObserver>();
		fPreschedulables = new ArrayList<Schedulable>();
	}

	/**
	 * Adds a new {@link IEventObserver}.
	 * 
	 * @param observer
	 *            observer to be added.
	 * 
	 * @param type
	 *            the type of event it is interested in
	 * 
	 * @param binding
	 *            whether the observer is <b>binding</b> or not. Binding
	 *            observers will preclude simulation from halting until they
	 *            call {@link ISimulationEngine#unbound(IEventObserver)}
	 * 
	 * @param listening
	 *            non-listening observers will not be notified by the engine
	 *            when an event of a given type occurs. This is useful when the
	 *            observer is binding, but gets its events from other channels
	 *            for efficiency reasons (e.g. per-process
	 *            {@link IEventObserver}s can be made binding and
	 *            non-listening).
	 */
	public void addObserver(IEventObserver observer, int type, boolean binding,
			boolean listening) {
		fEventObservers.add(new Descriptor(observer, type, binding, listening));
		fBinding += binding ? 1 : 0;
	}

	public void addObserver(ILifecycleObserver observer) {
		fLifecycleObservers.add(observer);
	}

	public void addProcess(IProcess... processes) {
		for (IProcess process : processes) {
			fProcesses.add(process);
		}
	}

	public void preschedule(Schedulable... schedulables) {
		for (Schedulable schedulable : schedulables) {
			preschedule(schedulable);
		}
	}

	public void preschedule(Schedulable schedulable) {
		fPreschedulables.add(schedulable);
	}

	/**
	 * Creates a fixed-length simulation which will stop the engine at the
	 * predefined time instant.
	 * 
	 * @param type
	 *            the channel to which direct the stop notification, with the
	 *            associated {@link Anchor}, or -1 for no notification.
	 * @param time
	 *            the simulation time at which to stop the engine.
	 * @param trackProgress
	 *            whether to track simulation progress with an
	 *            {@link IProgressTracker}.
	 */
	public void stopAt(int type, double time, boolean trackProgress) {
		preschedule(new TimedProgressTracker(time, -1), // tracks progress
				new Anchor(time, type)); // stops simulation cold
	}

	/**
	 * Registers an {@link IEventObserver} which will be triggered when the
	 * burnin period at the engine finishes.
	 * 
	 * @param observer
	 *            the observer to be triggered.
	 */
	public void addBurninAction(final IEventObserver observer) {
		preschedule(new Schedulable() {

			private static final long serialVersionUID = 1L;

			@Override
			public int type() {
				return -1;
			}

			@Override
			public double time() {
				return fBurnin;
			}

			@Override
			public void scheduled(ISimulationEngine engine) {
				observer.eventPerformed(engine, this, IEventObserver.EXPIRED);
			}

			@Override
			public boolean isExpired() {
				return true;
			}
		});
	}

	/**
	 * @param burnin
	 *            the burning period for this simulation, during which no
	 *            observers will be notified of events by the engine.
	 */
	public void setBurnin(double burnin) {
		fBurnin = burnin;
	}

	/**
	 * @param permits
	 *            sets the number of extra permits for the engine to be built.
	 */
	public void setExtraPermits(int permits) {
		fExtraPermits = permits;
	}

	/**
	 * @return an estimate of the number of permits this engine has.
	 */
	public int permits() {
		return fBinding + fExtraPermits;
	}

	/**
	 * Constructs an {@link EDSimulationEngine}, based on the parameters set on
	 * the builder. This method can only be called once.
	 * 
	 * @return a new {@link EDSimulationEngine}.
	 */
	public EDSimulationEngine engine() {
		if (fInstance != null) {
			throw new IllegalStateException(
					"This builder can only build one engine.");
		}

		// Guard against it as it's hardly the case that this would be intended.
		if (permits() == 0) {
			throw new IllegalStateException("The configured engine has no "
					+ "permits.");
		}

		IProcess[] processes = new IProcess[fProcesses.size()];
		for (int i = 0; i < processes.length; i++) {
			processes[i] = fProcesses.get(i);
		}

		fInstance = new EDSimulationEngine(
				processes,
				fEventObservers.toArray(new Descriptor[fEventObservers.size()]),
				fLifecycleObservers
						.toArray(new ILifecycleObserver[fLifecycleObservers
								.size()]), fExtraPermits, fBurnin);

		// Let it be GC'ed.
		fProcesses = null;

		/*
		 * Transfer elements to the engine, while shrinking the list. This is
		 * useful when operating at the memory limit as it won't require storing
		 * two copies of a possibly huge list.
		 */
		int size = fPreschedulables.size();
		for (int i = fPreschedulables.size() - 1; i >= 0; i--) {
			fInstance.schedule(fPreschedulables.remove(i));
			if (size > RESIZE_THRESHOLD && i < size / 2) {
				fPreschedulables.trimToSize();
				size = fPreschedulables.size();
			}
		}

		// Again, let the GC do its work.
		fPreschedulables = null;

		return fInstance;
	}

	/**
	 * @return a references to a (possibly yet unbuilt)
	 *         {@link ISimulationEngine}. This will resolve to <code>null</code>
	 *         until {@link #engine()} is called.
	 */
	public IReference<ISimulationEngine> reference() {
		return new IReference<ISimulationEngine>() {
			@Override
			public ISimulationEngine get() {
				return fInstance;
			}
		};
	}

	// -------------------------------------------------------------------------

}
