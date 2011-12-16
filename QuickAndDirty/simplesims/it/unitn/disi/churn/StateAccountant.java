package it.unitn.disi.churn;

import peersim.util.IncrementalStats;

public class StateAccountant {

	private final IncrementalStats fPermanence;

	private final IncrementalStats fTimeToHit;

	private double fLastEnter;

	private double fLastExit;

	public StateAccountant() {
		this(new IncrementalStats(), new IncrementalStats());
	}

	public StateAccountant(IncrementalStats permanence,
			IncrementalStats timeToHit) {
		fPermanence = permanence; 
		fTimeToHit = timeToHit;
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
