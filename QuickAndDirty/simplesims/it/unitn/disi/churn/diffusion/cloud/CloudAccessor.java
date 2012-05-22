package it.unitn.disi.churn.diffusion.cloud;

import it.unitn.disi.churn.diffusion.HFlood;
import it.unitn.disi.simulator.INetwork;
import it.unitn.disi.simulator.IProcess;
import it.unitn.disi.simulator.IProcess.State;
import it.unitn.disi.simulator.IProcessObserver;
import it.unitn.disi.simulator.PausingCyclicProtocolRunner;
import it.unitn.disi.simulator.Schedulable;
import it.unitn.disi.simulator.SimpleEDSim;
import it.unitn.disi.simulator.random.IDistribution;

/**
 * {@link CloudAccessor} will access the "cloud" and activate the dissemination
 * process over the {@link HFlood} instance to which it is linked. This simple
 * implementation supports a "one-shot" access.
 * 
 * @author giuliano
 */
public class CloudAccessor implements IProcessObserver {

	public static final int UNFIRED = 0;

	public static final int SUPPRESSED = 1;

	public static final int ACCESSED = 2;

	private final PausingCyclicProtocolRunner<HFlood> fRunner;

	private final IDistribution fDistribution;

	private final HFlood fSource;

	private final HFlood fDelegate;

	private final SimpleEDSim fSim;

	private double fRemaining;

	private int fOutcome = UNFIRED;

	public CloudAccessor(IDistribution distribution, HFlood source,
			HFlood delegate, PausingCyclicProtocolRunner<HFlood> runner,
			SimpleEDSim sim) {
		fDistribution = distribution;
		fSource = source;
		fDelegate = delegate;
		fRunner = runner;
		fSim = sim;

		reset();
	}

	public int outcome() {
		return fOutcome;
	}

	@Override
	public void stateShifted(IProcess process, State state, double time,
			double next) {

		// If our node is going down or we have already fired
		// this accessor, just returns.
		if (state == State.down || fOutcome != UNFIRED) {
			return;
		}

		// Checks if our timer will expire during this section.
		double length = next - time;
		if (fRemaining > length) {
			// Nope, just return.
			fRemaining -= length;
			// Sanity check.
			return;
		}

		// It will, so we schedule a cloud access for when it does.
		fSim.schedule(new CloudAccess(time + fRemaining));

	}

	private void reset() {
		fRemaining = fDistribution.sample();
	}

	private class CloudAccess extends Schedulable {

		private final double fTime;

		public CloudAccess(double time) {
			fTime = time;
		}

		@Override
		public void scheduled(double time, INetwork parent) {
			// Resets the timer.
			reset();

			// If the source hasn't been reached, doesn't do anything.
			if (!fSource.isReached()) {
				return;
			}

			if (fOutcome != UNFIRED) {
				throw new IllegalStateException();
			}

			if (fDelegate.isReached()) {
				System.err.println(fDelegate.id() + " is suppressed.");
				fOutcome = SUPPRESSED;
			} else {
				System.err.println(fDelegate.id() + " accessed the cloud.");
				fOutcome = ACCESSED;
				// Accessing the cloud means reaching the node out of nowhere.
				fDelegate.markReached(time);
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

}
