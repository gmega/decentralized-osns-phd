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

	private final State fState;

	private final int fId;

	public FixedProcess(int id, State state) {
		super();
		fState = state;
		fId = id;
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
		throw new IllegalStateException(
				"Fixed processes should not be scheduled.");
	}

	@Override
	public double time() {
		return Double.MAX_VALUE;
	}

}
