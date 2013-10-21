package it.unitn.disi.simulator.util;

import it.unitn.disi.simulator.core.ISimulationEngine;
import it.unitn.disi.simulator.core.Schedulable;

public class Anchor extends Schedulable {

	private static final long serialVersionUID = 1L;
	
	private double fStopTime;
	
	private int fType;
	
	public Anchor(double stopTime, int type) {
		fStopTime = stopTime;
		fType = type;
	}

	@Override
	public boolean isExpired() {
		return false;
	}

	@Override
	public void scheduled(ISimulationEngine state) {
		state.stop(state.stopPermits());
	}

	@Override
	public double time() {
		return fStopTime;
	}

	@Override
	public int type() {
		return fType;
	}

}
