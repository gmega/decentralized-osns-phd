package it.unitn.disi.churn.simulator;

import peersim.config.AutoConfig;

@AutoConfig
public class PeriodicSchedulable extends Schedulable {

	private double fPeriod;

	private double fTime = 0;
	
	private int fType;
	
	public PeriodicSchedulable(double period, int type) {
		fPeriod = period;
		fType = type;
	}
	
	@Override
	public void scheduled(double time, SimpleEDSim parent) {
		nextPeriod(time, parent);
		fTime += fPeriod;
	}

	@Override
	public double time() {
		return fTime;
	}

	@Override
	public boolean isExpired() {
		return false;
	}

	protected void nextPeriod(double time, SimpleEDSim parent) {
		// To be overriden by subclasses.
	}

	@Override
	public int type() {
		return fType;
	}

}

