package it.unitn.disi.churn.simulator;

import it.unitn.disi.random.IDistribution;

public class RenewalProcess extends IProcess {

	private final int fId;

	private final IDistribution fUp;

	private final IDistribution fDown;

	private State fState;

	private double fNextEvent;

	private double fUptime;

	public RenewalProcess(int id, IDistribution upDown, State initial) {
		this(id, upDown, upDown, initial, null);
	}

	public RenewalProcess(int id, IDistribution up, IDistribution down,
			State initial) {
		this(id, up, down, initial, null);
	}

	public RenewalProcess(int id, IDistribution up, IDistribution down,
			State initial, Object[] protocols) {
		super(protocols);
		fId = id;
		fUp = up;
		fDown = down;
		fState = initial;
	}

	// -------------------------------------------------------------------------
	// Schedulable interface.
	// -------------------------------------------------------------------------

	public void scheduled(double time, INetwork sim) {
		double increment = 0;
		switch (fState) {

		case down:
			increment = fUp.sample();
			fState = State.up;
			// Overestimate of uptime.
			fUptime += increment;
			break;

		case up:
			increment = fDown.sample();
			fState = State.down;
			break;

		}

		fNextEvent += increment;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.unitn.disi.churn.simulator.IProcess#id()
	 */
	@Override
	public boolean isExpired() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.unitn.disi.churn.simulator.IProcess#id()
	 */
	@Override
	public double time() {
		return fNextEvent;
	}

	// -------------------------------------------------------------------------
	// IProcess interface.
	// -------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * it.unitn.disi.churn.simulator.IProcess#uptime(it.unitn.disi.churn.simulator
	 * .SimpleEDSim)
	 */
	@Override
	public double uptime(SimpleEDSim parent) {
		double delta = 0;
		if (isUp()) {
			// Corrects the uptime overestimation.
			delta = fNextEvent - parent.currentTime();
		}

		// Makes sure we're not doing anything funky.
		if (fUptime - delta < 0) {
			throw new IllegalStateException("Internal error.");
		}

		return fUptime - delta;
	}

	@Override
	/*
	 * (non-Javadoc)
	 * 
	 * @see it.unitn.disi.churn.simulator.IProcess#state()
	 */
	public boolean isUp() {
		return state() == State.up;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.unitn.disi.churn.simulator.IProcess#state()
	 */
	@Override
	public State state() {
		return fState;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.unitn.disi.churn.simulator.IProcess#id()
	 */
	@Override
	public int id() {
		return fId;
	}

	// -------------------------------------------------------------------------

	public String toString() {
		return "[" + fId + ", " + fState + "]";
	}

}
