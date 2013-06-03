package it.unitn.disi.simulator.core;

import it.unitn.disi.simulator.core.EDSimulationEngine.Descriptor;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link EngineBuilder} allows clients to instantiate
 * {@link EDSimulationEngine}.
 * 
 * @author giuliano
 */
public class EngineBuilder {

	private List<Descriptor> fObservers;

	private List<IProcess> fProcesses;
	
	private int fExtraPermits;

	private double fBurnin;

	private boolean fUsed;

	/**
	 * Creates a new {@link EngineBuilder}. A builder can only build a single
	 * engine.
	 */
	public EngineBuilder() {
		fObservers = new ArrayList<Descriptor>();
		fProcesses = new ArrayList<IProcess>();
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
		fObservers.add(new Descriptor(observer, type, binding, listening));
	}

	public void addProcess(IProcess... processes) {
		for (IProcess process : processes) {
			fProcesses.add(process);
		}
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
		return countBinding(fObservers) + fExtraPermits;
	}

	/**
	 * Constructs an {@link EDSimulationEngine}, based on the parameters set on
	 * the builder. This method can only be called once.
	 * 
	 * @return a new {@link EDSimulationEngine}.
	 */
	public EDSimulationEngine engine() {
		if (fUsed) {
			throw new IllegalStateException(
					"This builder can only build one engine.");
		}

		IProcess[] processes = new IProcess[fProcesses.size()];
		for (int i = 0; i < processes.length; i++) {
			processes[i] = fProcesses.get(i);
		}

		fUsed = true;

		return new EDSimulationEngine(
				fProcesses.toArray(new IProcess[fProcesses.size()]),
				fObservers.toArray(new Descriptor[fObservers.size()]),
				fExtraPermits, fBurnin);
	}

	private int countBinding(List<Descriptor> observers) {
		int count = 0;
		for (Descriptor descriptor : observers) {
			if (descriptor.binding) {
				count++;
			}
		}

		return count;
	}

	// -------------------------------------------------------------------------

}
