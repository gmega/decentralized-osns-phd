package it.unitn.disi.churn.diffusion.cloud;

import it.unitn.disi.simulator.random.IDistribution;

public class FixedPeriodDistribution implements IDistribution {
	
	private final double fPeriod;
	
	private double fDelay;
	
	public FixedPeriodDistribution(double delay, double period) {
		fPeriod = period;
		fDelay = delay;
	}

	@Override
	public double sample() {
		return fDelay < 0 ? fPeriod : fDelay; 
	}

	@Override
	public double expectation() {
		return fPeriod;
	}

}
