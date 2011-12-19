package it.unitn.disi.churn;

import peersim.util.IncrementalStats;
import it.unitn.disi.random.IDistribution;

public class RenewalProcess implements Comparable<RenewalProcess> {

	public static enum State {
		up, down;
	}

	private final int fId;

	private IncrementalStats fSession;

	private IncrementalStats fInterSession;

	private final IDistribution fUp;

	private final IDistribution fDown;

	private State fState;

	private double fNextEvent;

	private int fN;

	public RenewalProcess(int id, IDistribution upDown, State initial) {
		this(id, upDown, upDown, initial);
	}

	public RenewalProcess(int id, IDistribution up, IDistribution down,
			State initial) {
		fId = id;
		fUp = up;
		fDown = down;
		fState = initial;
		fSession = new IncrementalStats();
		fInterSession = new IncrementalStats();
	}

	public void next() {
		double increment = 0;
		switch (fState) {

		case down:
			increment = fUp.sample();
			fState = State.up;
			fSession.add(increment);
			break;

		case up:
			increment = fDown.sample();
			fState = State.down;
			fInterSession.add(increment);
			break;

		}

		fNextEvent += increment;
		fN++;
	}

	public boolean isUp() {
		return state() == State.up;
	}

	public State state() {
		return fState;
	}

	public int jumps() {
		return fN;
	}

	public double nextSwitch() {
		return fNextEvent;
	}

	@Override
	public int compareTo(RenewalProcess o) {
		return (int) Math.signum(this.nextSwitch() - o.nextSwitch());
	}

	public IncrementalStats upStats() {
		return fSession;
	}

	public IncrementalStats downStats() {
		return fInterSession;
	}

	public int id() {
		return fId;
	}

}
