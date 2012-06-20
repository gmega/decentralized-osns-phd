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
 * Very simple experiment for sampling the synchronization time of two nodes.
 * 
 * @author giuliano
 */
@Binding
public class TrueSyncEstimator implements IEventObserver {

	private EDSimulationEngine fParent;

	private volatile int fSamples;

	private final IValueObserver fObserver;

	private final TDoubleArrayList fPendingUps;

	private final boolean fCloud;

	public TrueSyncEstimator(EDSimulationEngine engine, int samples,
			boolean cloud, IValueObserver observer) {
		fSamples = samples;
		fPendingUps = new TDoubleArrayList();
		fObserver = observer;
		fCloud = cloud;
		fParent = engine;
	}

	@Override
	public void eventPerformed(ISimulationEngine state,
			Schedulable schedulable, double nextShift) {

		IProcess process = (IProcess) schedulable;
		INetwork p = state.network();

		double time = state.clock().time();

		// We saw a login event for P1.
		if (process.id() == p.process(0).id() && process.isUp()) {
			fPendingUps.add(time);
		}

		// P1 and P2 are synchronized.
		if (senderUp(p) && p.process(1).isUp()) {
			register(time, fPendingUps);
			fPendingUps.resetQuick();
		}

		if (isDone()) {
			fParent.unbound(this);
		}
	}

	protected void register(double p2Login, TDoubleArrayList p1Logins) {
		for (int i = 0; i < p1Logins.size() && fSamples > 0; i++, fSamples--) {
			fObserver.observe(p2Login - p1Logins.get(i));
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
