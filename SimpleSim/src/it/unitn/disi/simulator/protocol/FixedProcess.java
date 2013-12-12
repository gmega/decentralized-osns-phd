package it.unitn.disi.simulator.protocol;

import it.unitn.disi.simulator.core.IClockData;
import it.unitn.disi.simulator.core.IProcess;
import it.unitn.disi.simulator.core.ISimulationEngine;

/**
 * {@link FixedProcess} is a special process which never switches state.
 * 
 * @author giuliano
 */
public class FixedProcess extends IProcess {

	private static final long serialVersionUID = 1L;

	private final State fState;

	private final int fId;

	private boolean fAnnounce;

	public FixedProcess(int id, State state, boolean announce) {
		super();
		fState = state;
		fId = id;
		fAnnounce = announce;
	}

	public FixedProcess(int id, State state) {
		this(id, state, false);
	}

	@Override
	public double uptime(IClockData clock) {
		switch (fState) {
		case down:
			return 0;
		case up:
			return clock.rawTime();
		}
		throw new IllegalStateException("Internal error.");
	}

	@Override
	public double asymptoticAvailability() {
		return (fState == State.up) ? 1.0 : 0.0;
	}

	@Override
	public boolean isUp() {
		return fState == State.up;
	}

	@Override
	public State state() {
		return fState;
	}

	@Override
	public int id() {
		return fId;
	}

	@Override
	public boolean isExpired() {
		return false;
	}

	@Override
	public void scheduled(ISimulationEngine state) {
		if (!fAnnounce) {
			throw new IllegalStateException("Error: fixed processes "
					+ "should not be scheduled beyond the "
					+ "initial announcement.");
		}

		fAnnounce = false;
		
		notifyObservers(state, time());
	}

	@Override
	public double time() {
		return fAnnounce ? 0 : Double.POSITIVE_INFINITY;
	}

}
