package it.unitn.disi.churn;

import peersim.util.IncrementalStats;
import it.unitn.disi.random.IDistribution;

public class RenewalProcess implements Comparable<RenewalProcess> {

	public static enum State {
		up, down;
	}
	
	private IncrementalStats fSession;
	
	private IncrementalStats fInterSession;

	private final IDistribution fUp;

	private final IDistribution fDown;

	private State fState;

	private double fNextEvent;
	
	private int fN;

	public RenewalProcess(IDistribution upDown, State initial) {
		this(upDown, upDown, initial);
	}

	public RenewalProcess(IDistribution up, IDistribution down, State initial) {
		fUp = up;
		fDown = down;
		fState = initial;
		fSession = new IncrementalStats();
		fInterSession = new IncrementalStats();
	}

	public boolean isUp() {
		return fState == State.up;
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

}
