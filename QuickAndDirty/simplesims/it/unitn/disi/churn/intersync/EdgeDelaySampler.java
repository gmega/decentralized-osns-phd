package it.unitn.disi.churn.intersync;

import gnu.trove.list.array.TDoubleArrayList;
import it.unitn.disi.simulator.core.Binding;
import it.unitn.disi.simulator.core.EDSimulationEngine;
import it.unitn.disi.simulator.core.INetwork;
import it.unitn.disi.simulator.core.IProcess;
import it.unitn.disi.simulator.core.IEventObserver;
import it.unitn.disi.simulator.core.Schedulable;
import it.unitn.disi.simulator.core.ISimulationEngine;
import it.unitn.disi.simulator.measure.IValueObserver;

/**
 * Very simple experiment for sampling edge delay.
 * 
 * @author giuliano
 */
@Binding
public class EdgeDelaySampler implements IEventObserver {

	private static final long serialVersionUID = 1L;

	private EDSimulationEngine fParent;

	private volatile int fSamples;

	private final IValueObserver fObserver;

	private final TDoubleArrayList fPendingUps;

	private final boolean fCloud;

	public EdgeDelaySampler(int samples, boolean cloud, IValueObserver observer) {
		fSamples = samples;
		fPendingUps = new TDoubleArrayList();
		fObserver = observer;
		fCloud = cloud;
	}

	@Override
	public void eventPerformed(ISimulationEngine engine,
			Schedulable schedulable, double nextShift) {

		IProcess process = (IProcess) schedulable;
		INetwork p = engine.network();

		double time = engine.clock().time();

		// We saw a login event for P1.
		if (process.id() == p.process(0).id() && process.isUp()) {
			fPendingUps.add(time);
		}

		// P1 and P2 are synchronized.
		if (senderUp(p) && p.process(1).isUp()) {
			register(time, fPendingUps, engine);
			fPendingUps.resetQuick();
		}

		if (isDone()) {
			engine.unbound(this);
		}
	}

	protected void register(double p2Login, TDoubleArrayList p1Logins,
			ISimulationEngine engine) {
		for (int i = 0; i < p1Logins.size() && fSamples > 0; i++, fSamples--) {
			fObserver.observe(p2Login - p1Logins.get(i), engine);
		}
	}

	private boolean senderUp(INetwork p) {
		return fCloud || p.process(0).isUp();
	}

	@Override
	public boolean isDone() {
		return fSamples <= 0;
	}

}
