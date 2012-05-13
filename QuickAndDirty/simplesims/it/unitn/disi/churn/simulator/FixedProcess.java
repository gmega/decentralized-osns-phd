package it.unitn.disi.churn.simulator;

/**
 * {@link FixedProcess} is a special process which never switches state.
 * 
 * @author giuliano
 */
public class FixedProcess extends IProcess {

	private final State fState;

	private final int fId;

	public FixedProcess(int id, State state) {
		this(id, state, null);
	}

	public FixedProcess(int id, State state, Object[] protocols) {
		super(protocols);
		fState = state;
		fId = id;
	}

	@Override
	public double uptime(SimpleEDSim parent) {
		switch (fState) {
		case down:
			return 0;
		case up:
			return parent.currentTime();
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
	public void scheduled(double time, INetwork parent) {
		throw new IllegalStateException(
				"Fixed processes should not be scheduled.");
	}

	@Override
	public double time() {
		return Double.MAX_VALUE;
	}

}
