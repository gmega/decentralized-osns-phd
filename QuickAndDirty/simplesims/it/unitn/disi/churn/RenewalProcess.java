package it.unitn.disi.churn;

import it.unitn.disi.random.IDistribution;

public class RenewalProcess implements Comparable<RenewalProcess> {

	public static enum State {
		up, down;
	}

	private final int fId;

	private final IDistribution fUp;

	private final IDistribution fDown;

	private State fState;

	private double fNextEvent;

	public RenewalProcess(int id, IDistribution upDown, State initial) {
		this(id, upDown, upDown, initial);
	}

	public RenewalProcess(int id, IDistribution up, IDistribution down,
			State initial) {
		fId = id;
		fUp = up;
		fDown = down;
		fState = initial;
	}

	public void next() {
		double increment = 0;
		switch (fState) {

		case down:
			increment = fUp.sample();
			fState = State.up;
			break;

		case up:
			increment = fDown.sample();
			fState = State.down;
			break;

		}

		fNextEvent += increment;
	}

	public boolean isUp() {
		return state() == State.up;
	}

	public State state() {
		return fState;
	}

	public double nextSwitch() {
		return fNextEvent;
	}

	@Override
	public int compareTo(RenewalProcess o) {
		return (int) Math.signum(this.nextSwitch() - o.nextSwitch());
	}

	public int id() {
		return fId;
	}
	
	public String toString() {
		return "[" + fId + ", " + fState + "]";
	}

}
