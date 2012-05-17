package it.unitn.disi.churn.intersync;

import it.unitn.disi.simulator.IEventObserver;
import it.unitn.disi.simulator.INetwork;
import it.unitn.disi.simulator.IProcess;
import it.unitn.disi.simulator.IValueObserver;
import it.unitn.disi.simulator.Schedulable;
import it.unitn.disi.simulator.SimpleEDSim;
import gnu.trove.list.array.TDoubleArrayList;

/**
 * Very simple experiment for sampling the synchronization time of two nodes.
 * 
 * @author giuliano
 */
public class TrueSyncEstimator implements IEventObserver {

	private SimpleEDSim fParent;
	
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
	public void simulationStarted(SimpleEDSim p) {
		fParent = p;
		fPId0 = p.process(0).id();
	}

	@Override
	public void stateShifted(INetwork p, double time, Schedulable schedulable) {

		IProcess process = (IProcess) schedulable;

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
			fParent.done(this);
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
