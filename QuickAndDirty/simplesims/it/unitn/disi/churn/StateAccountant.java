package it.unitn.disi.churn;

import peersim.util.IncrementalStats;

public class StateAccountant {

	private final IncrementalStats fPermanence = new IncrementalStats();

	private final IncrementalStats fTimeToHit = new IncrementalStats();

	private double fLastEnter;

	private double fLastExit;

	public StateAccountant() {

	}
	
	public void reset() {
		fPermanence.reset();
		fTimeToHit.reset();
	}

	public void enterState(double time) {
		fTimeToHit.add(check(time - fLastExit));
		fLastEnter = time;
	}

	public void exitState(double time) {
		fPermanence.add(check(time - fLastEnter));
		fLastExit = time;
	}

	private double check(double d) {
		if (d < 0) {
			throw new IllegalStateException();
		}
		return d;
	}
	
	public IncrementalStats permanence() {
		return fPermanence;
	}
	
	public IncrementalStats timeToHit() {
		return fTimeToHit;
	}
}
