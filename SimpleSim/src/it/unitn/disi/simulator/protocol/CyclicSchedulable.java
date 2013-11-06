package it.unitn.disi.simulator.protocol;

import it.unitn.disi.simulator.core.Schedulable;
import it.unitn.disi.simulator.core.ISimulationEngine;
import peersim.config.AutoConfig;

@AutoConfig
public class CyclicSchedulable extends Schedulable {

	private static final long serialVersionUID = 1L;

	protected final double fPeriod;

	private double fTime = 0;

	private int fType;
	
	public CyclicSchedulable(double period, int type) {
		fPeriod = period;
		fType = type;
	}

	@Override
	public void scheduled(ISimulationEngine state) {
		nextPeriod(state);
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

	protected void nextPeriod(ISimulationEngine state) {
		// To be overriden by subclasses.
	}

	protected void setTime(double time) {
		fTime = time;
	}

	@Override
	public int type() {
		return fType;
	}

}
