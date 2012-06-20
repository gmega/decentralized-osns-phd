package it.unitn.disi.churn.diffusion.cloud;

import it.unitn.disi.churn.diffusion.HFlood;
import it.unitn.disi.simulator.core.EDSimulationEngine;
import it.unitn.disi.simulator.core.IClockData;
import it.unitn.disi.simulator.core.IEventObserver;
import it.unitn.disi.simulator.core.IProcess;
import it.unitn.disi.simulator.core.IProcess.State;
import it.unitn.disi.simulator.core.Schedulable;
import it.unitn.disi.simulator.core.ISimulationEngine;
import it.unitn.disi.simulator.protocol.PausingCyclicProtocolRunner;
import it.unitn.disi.simulator.random.IDistribution;

/**
 * {@link CloudAccessor} will access the "cloud" and activate the dissemination
 * process over the {@link HFlood} instance to which it is linked. This simple
 * implementation supports a "one-shot" access.
 * 
 * @author giuliano
 */
public class CloudAccessor implements IEventObserver {

	public static final int UNFIRED = 0;

	public static final int SUPPRESSED = 1;

	public static final int ACCESSED = 2;

	public static final int UPTIME = 0;

	public static final int WALLCLOCK = 1;

	private final PausingCyclicProtocolRunner<HFlood> fRunner;

	private final IDistribution fDistribution;

	private final HFlood fSource;

	private final HFlood fDelegate;

	private final EDSimulationEngine fSim;

	private final int fClockMode;

	private double fRemaining;

	private int fOutcome = UNFIRED;

	public CloudAccessor(IDistribution distribution, HFlood source,
			HFlood delegate, PausingCyclicProtocolRunner<HFlood> runner,
			EDSimulationEngine sim, int clockMode) {
		fDistribution = distribution;
		fSource = source;
		fDelegate = delegate;
		fRunner = runner;
		fSim = sim;
		fClockMode = clockMode;

		reset(0.0);
	}

	public int outcome() {
		return fOutcome;
	}

	@Override
	public void eventPerformed(ISimulationEngine state, Schedulable schedulable,
			double nextShift) {

		State pState = ((IProcess) schedulable).state();

		// If our node is going down or we have already fired
		// this accessor, just returns.
		if (pState == State.down || fOutcome != UNFIRED) {
			return;
		}

		double accessTime = updateTimer(state.clock().rawTime(), nextShift);
		if (accessTime > 0) {
			// It will, so we schedule a cloud access for when it does.
			fSim.schedule(new CloudAccess(accessTime));
		}

	}

	private double updateTimer(double time, double next) {

		double expired = -1;

		switch (fClockMode) {

		// Timer is relative to uptime.
		case UPTIME:
			// Session length.
			double length = next - time;
			// Will timer expire during this session?
			if (fRemaining <= length) {
				// Yes, so we need to return exactly when.
				expired = time + fRemaining;
			} else {
				// Nope, just decrement the remaining time.
				fRemaining -= length;
			}
			break;

		case WALLCLOCK:
			// Has the timer expired while we were logged off?
			if (fRemaining < time) {
				// Yes, so we access the cloud immediately.
				expired = 0.0;
			}
			// Will it expire now?
			else if (fRemaining < next) {
				// Yes, schedule for the future.
				expired = next - fRemaining;
			}

			expired = fRemaining < time ? time : -1;
			break;
		}

		return expired;
	}

	private void reset(double time) {
		switch (fClockMode) {

		case UPTIME:
			fRemaining = fDistribution.sample();
			break;

		case WALLCLOCK:
			fRemaining = time + fDistribution.sample();
			break;
		}
	}

	private class CloudAccess extends Schedulable {

		private final double fTime;

		public CloudAccess(double time) {
			fTime = time;
		}

		@Override
		public void scheduled(ISimulationEngine state) {
			IClockData clock = state.clock();

			// Resets the timer.
			reset(clock.time());

			// If the source hasn't been reached, doesn't do anything.
			if (!fSource.isReached()) {
				return;
			}

			if (fOutcome != UNFIRED) {
				throw new IllegalStateException();
			}

			if (fDelegate.isReached()) {
				fOutcome = SUPPRESSED;
			} else {
				fOutcome = ACCESSED;
				// Accessing the cloud means reaching the node out of nowhere.
				fDelegate.markReached(clock);
				// Wakes up the protocol runner, otherwise it will keep on
				// sleeping and our node won't be scheduled for dissemination.
				fRunner.wakeUp();
			}
		}

		@Override
		public double time() {
			return fTime;
		}

		@Override
		public int type() {
			return Integer.MAX_VALUE;
		}

		@Override
		public boolean isExpired() {
			return true;
		}

	}

	@Override
	public boolean isDone() {
		return false;
	}

}
