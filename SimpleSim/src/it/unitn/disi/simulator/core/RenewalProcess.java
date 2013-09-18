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

	/**
	 * @return the asymptotic availability of this node, if defined.
	 */
	public double asymptoticAvailability() {
		double up = fUp.expectation();
		return up / (up + fDown.expectation());
	}

	/**
	 * @param clock
	 *            the simulation {@link IClockData}.
	 *            
	 * @return the empirical (simulated) availability for this node.
	 */
	public double empiricalAvailability(IClockData clock) {
		return uptime(clock) / clock.rawTime();
	}

	// -------------------------------------------------------------------------
	// Schedulable interface.
	// -------------------------------------------------------------------------

	public void scheduled(ISimulationEngine state) {
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
