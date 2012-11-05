package it.unitn.disi.simulator.measure;

import it.unitn.disi.simulator.core.ISimulationEngine;

public interface IValueObserver {

	public static IValueObserver NULL_OBSERVER = new IValueObserver() {
		@Override
		public void observe(double value, ISimulationEngine engine) {
		}
	};

	public void observe(double value, ISimulationEngine engine);

}
