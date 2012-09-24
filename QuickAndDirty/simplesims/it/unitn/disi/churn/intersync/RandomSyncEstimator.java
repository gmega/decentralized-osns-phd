package it.unitn.disi.churn.intersync;

import it.unitn.disi.simulator.core.Binding;
import it.unitn.disi.simulator.core.IEventObserver;
import it.unitn.disi.simulator.core.IProcess;
import it.unitn.disi.simulator.core.ISimulationEngine;
import it.unitn.disi.simulator.core.Schedulable;
import it.unitn.disi.simulator.measure.IValueObserver;

@Binding
public class RandomSyncEstimator implements IEventObserver {

	private final IValueObserver fObserver;

	private volatile boolean fDone;

	public RandomSyncEstimator(IValueObserver observer) {
		fObserver = observer;
	}

	@Override
	public void eventPerformed(ISimulationEngine engine,
			Schedulable schedulable, double nextShift) {

		IProcess p1 = engine.network().process(0);
		IProcess p2 = engine.network().process(1);

		if (p1.isUp() && p2.isUp()) {
			fObserver.observe(engine.clock().time());
			fDone = true;
			engine.unbound(this);
		}
	}

	@Override
	public boolean isDone() {
		return fDone;
	}

}
