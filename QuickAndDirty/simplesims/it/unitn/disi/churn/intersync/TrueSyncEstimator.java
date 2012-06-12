package it.unitn.disi.churn.intersync;

import gnu.trove.list.array.TDoubleArrayList;
import it.unitn.disi.simulator.core.EDSimulationEngine;
import it.unitn.disi.simulator.core.INetwork;
import it.unitn.disi.simulator.core.IProcess;
import it.unitn.disi.simulator.core.ISimulationObserver;
import it.unitn.disi.simulator.core.Schedulable;
import it.unitn.disi.simulator.core.SimulationState;
import it.unitn.disi.simulator.measure.IValueObserver;

/**
 * Very simple experiment for sampling the synchronization time of two nodes.
 * 
 * @author giuliano
 */
public class TrueSyncEstimator implements ISimulationObserver {

	private EDSimulationEngine fParent;
	
	private volatile int fSamples;

	private final IValueObserver fObserver;

	private final TDoubleArrayList fPendingUps;

	private final boolean fCloud;

	private volatile int fPId0;

	public TrueSyncEstimator(int samples, boolean cloud, IValueObserver observer) {
		fSamples = samples;
		fPendingUps = new TDoubleArrayList();
		fObserver = observer;
		fCloud = cloud;
	}

	@Override
	public void simulationStarted(EDSimulationEngine p) {
		fParent = p;
		fPId0 = p.process(0).id();
	}

	@Override
	public void eventPerformed(SimulationState state, Schedulable schedulable) {

		IProcess process = (IProcess) schedulable;
		INetwork p = state.network();

		double time = state.clock().time();
		
		// We saw a login event for P1.
		if (process.id() == fPId0 && process.isUp()) {
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

	@Override
	public boolean isBinding() {
		return true;
	}

}
