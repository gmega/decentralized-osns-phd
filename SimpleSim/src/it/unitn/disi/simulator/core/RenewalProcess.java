package it.unitn.disi.simulator.core;

import java.io.Serializable;

import it.unitn.disi.simulator.random.IDistribution;

public class RenewalProcess extends IProcess implements Serializable {

	private static final long serialVersionUID = -5308561729336795474L;

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

	@Override
	public double asymptoticAvailability() {
		double up = fUp.expectation();
		return up / (up + fDown.expectation());
	}

	/**
	 * Changes this renewal process into the specified {@link State}, causing a
	 * renewal and all corresponding statistics to be updated. Calling this
	 * method directly is only safe <b>before the simulation begins</b>.<BR>
	 * <BR>
	 * Calling it during the simulation can break the alternating properties of
	 * {@link RenewalProcess} to external observers, and calling it outside of
	 * scheduling bounds will lead to wrong estimates of uptime. Further, it
	 * will not cause {@link IEventObserver}s to be notified, which can lead to
	 * further inconsistencies. You have been warned. ;-)
	 * 
	 * @param state
	 *            the new state in which to transition this
	 *            {@link RenewalProcess}.
	 */
	public void changeState(State state) {
		double increment = 0;

		switch (state) {

		case up:
			increment = fUp.sample();
			// Overestimate of uptime.
			fUptime += increment;
			break;

		case down:
			increment = fDown.sample();
			break;
		}

		fState = state;
		fNextEvent += increment;
	}

	// -------------------------------------------------------------------------
	// Schedulable interface.
	// -------------------------------------------------------------------------

	public void scheduled(ISimulationEngine state) {
		changeState(fState.opposite());
		notifyObservers(state, fNextEvent);
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
	public double uptime(IClockData clock) {
		double delta = 0;
		if (isUp()) {
			// Corrects the uptime overestimation.
			delta = fNextEvent - clock.rawTime();
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
