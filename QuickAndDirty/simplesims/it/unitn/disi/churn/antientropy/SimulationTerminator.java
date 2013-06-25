package it.unitn.disi.churn.antientropy;

import it.unitn.disi.simulator.core.ISimulationEngine;
import it.unitn.disi.simulator.core.Schedulable;

public class SimulationTerminator extends Schedulable {

	private static final long serialVersionUID = 1L;
	
	private double fTime;
	
	public SimulationTerminator(double time) {
		fTime = time;
	}

	@Override
	public boolean isExpired() {
		return false;
	}

	@Override
	public void scheduled(ISimulationEngine engine) {
		engine.stop();
	}

	@Override
	public double time() {
		return fTime;
	}

	@Override
	public int type() {
		return Integer.MAX_VALUE;
	}
	

}
