package it.unitn.disi.churn;

import it.unitn.disi.simulator.measure.IValueObserver;

public class StateAccountant {

	private final IValueObserver fPermanence;

	private final IValueObserver fTimeToHit;

	private double fLastEnter;

	private double fLastExit;

	public StateAccountant(IValueObserver permanence,
			IValueObserver timeToHit) {
		fPermanence = permanence; 
		fTimeToHit = timeToHit;
	}

	public void enterState(double time) {
		fTimeToHit.observe(check(time - fLastExit), null);
		fLastEnter = time;
	}

	public void exitState(double time) {
		fPermanence.observe(check(time - fLastEnter), null);
		fLastExit = time;
	}

	private double check(double d) {
		if (d < 0) {
			throw new IllegalStateException();
		}
		return d;
	}

}
