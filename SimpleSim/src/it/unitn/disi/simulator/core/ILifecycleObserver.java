package it.unitn.disi.simulator.core;

/**
 * {@link ILifecycleObserver} are interested in knowing when the simulation
 * engine pauses/resumes, and when it runs out of stop permits.
 * 
 * @author giuliano
 */
public interface ILifecycleObserver {

	public static enum Type {
		running, halted, done
	}

	/**
	 * Called when one of the relevant lifecycle event happens.
	 * 
	 * @param engine
	 *            the engine in which the event has happened.
	 * @param evt
	 *            the event type (see {@link Type}).
	 */
	public void lifecycleEvent(ISimulationEngine engine, Type evt);
}
