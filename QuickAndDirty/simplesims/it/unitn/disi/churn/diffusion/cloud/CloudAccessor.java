package it.unitn.disi.churn.diffusion.cloud;

import java.util.Arrays;

import it.unitn.disi.churn.diffusion.IDisseminationService;
import it.unitn.disi.churn.diffusion.IMessageObserver;
import it.unitn.disi.churn.diffusion.HFloodMMsg;
import it.unitn.disi.simulator.core.IClockData;
import it.unitn.disi.simulator.core.IEventObserver;
import it.unitn.disi.simulator.core.IProcess;
import it.unitn.disi.simulator.core.IProcess.State;
import it.unitn.disi.simulator.core.Schedulable;
import it.unitn.disi.simulator.core.ISimulationEngine;

/**
 * {@link CloudAccessor} will access the "cloud" at the expiration of a timer,
 * and produce a new post, whenever there are updates.
 * 
 * @author giuliano
 */
public class CloudAccessor implements IEventObserver, IMessageObserver {

	private static final boolean DEBUG = false;

	private static final double ROUNDOFF_SUM = 0.0000000000001D;

	private final IDisseminationService fDisseminationService;

	private final ICloud fCloud;

	private final ISimulationEngine fSim;

	private final double fTimerPeriod;

	private final int fId;

	private CloudAccess fAccessor;

	private double fNextAccess;

	private double fNextShift;
	
	private double fLastAccess;

	/**
	 * Constructs a new {@link CloudAccessor}.
	 * 
	 * @param sim
	 *            reference to the enclosing {@link ISimulationEngine}.
	 * @param disseminationService
	 *            {@link IDisseminationService} used to send messages.
	 * @param cloud
	 *            {@link ICloud} reference from where we fetch updates.
	 * @param period
	 *            period with which we are willing to wait without updates
	 *            before going for the cloud.
	 * @param delay
	 *            initial delay for which we won't go for the cloud, regardless
	 *            of the access period. Useful to specify a burnin period.
	 * @param id
	 *            id of the current node.
	 */
	public CloudAccessor(ISimulationEngine sim,
			IDisseminationService disseminationService, ICloud cloud,
			double period, double delay, int id) {
		fDisseminationService = disseminationService;
		fSim = sim;
		fCloud = cloud;
		fId = id;
		fTimerPeriod = period;
		fNextAccess = fTimerPeriod + delay;
	}

	@Override
	public void eventPerformed(ISimulationEngine state,
			Schedulable schedulable, double nextShift) {

		State pState = ((IProcess) schedulable).state();

		// If our node is going down, we have nothing to do.
		if (pState == State.down) {
			fNextShift = -1;
			return;
		}

		fNextShift = nextShift;
		scheduleCloudAccess(nextShift, state.clock());
	}

	private void updateKnowledge(double lastSeen, IClockData clock) {
		fNextAccess = Math.max(fNextAccess, lastSeen + fTimerPeriod);
		if (DEBUG) {
			System.err.println("(" + clock.rawTime() + "): node " + fId
					+ " update last seen to " + lastSeen + ". Next access at "
					+ fNextAccess + ".");
		}
		scheduleCloudAccess(fNextShift, clock);
	}

	private void scheduleCloudAccess(double nextShift, IClockData clock) {
		// We schedule the accessor if the next access is to
		// happen during the current session.
		boolean access = fNextAccess < nextShift;

		// Yet, it could be that:
		// 1. we had a scheduled access, but got information regarding
		// recent updates. In this case:
		if (scheduled()) {
			// 1a. if the access state changed (i.e. a message came in
			// from someone telling us they know a very recent update)
			// to a point in which the next access has moved beyond the
			// current session, we just postpone it.
			if (!access) {
				fAccessor.cancel();
			}

			// 1b. Otherwise, we just nudge it by the amount required.
			else if (fNextAccess != fAccessor.time()) {
				fAccessor.cancel();
				scheduleAccess(clock);
			}
		}

		// 2. We had no scheduled access. In this case:
		else {
			// 2a. if the access state changed, we schedule an access.
			if (access) {
				scheduleAccess(clock);
			}
		}
	}

	private void scheduleAccess(IClockData clock) {
		fAccessor = new CloudAccess(Math.max(clock.rawTime(), fNextAccess));
		fSim.schedule(fAccessor);
	}

	@Override
	public void messageReceived(HFloodMMsg message, IClockData clock) {
		// Sanity check.
		if (fNextShift < 0) {
			throw new IllegalStateException(
					"Can't receive message while offline");
		}

		if (DEBUG) {
			System.err.println("(" + clock.rawTime() + "): node " + fId
					+ " received [" + message.toString()
					+ "] from P2P network.");
		}

		updateKnowledge(message.timestamp(), clock);
	}

	@Override
	public boolean isDone() {
		return false;
	}

	private boolean scheduled() {
		return fAccessor != null && !fAccessor.isExpired();
	}

	private class CloudAccess extends Schedulable {

		private final double fTime;

		private boolean fDone;

		public CloudAccess(double time) {
			fTime = time;
		}

		@Override
		public void scheduled(ISimulationEngine engine) {
			if (fDone) {
				return;
			}
			
			if (!fSim.network().process(fId).isUp()) {
				throw new IllegalStateException("A node that is down cannot access the cloud.");
			}
			
			fDone = true;
			IClockData clock = engine.clock();

			// Need the roundoff sum to avoid a roundoff error causing us to
			// query behind the timestamp for an update we already know.
			double queryTimestamp = fNextAccess - fTimerPeriod + ROUNDOFF_SUM;

			// Fetch updates.
			HFloodMMsg[] updates = fCloud.fetchUpdates(fId, -1, queryTimestamp);
			//System.err.println("Cloud access.");
			if (DEBUG) {
				System.err.println("("
						+ clock.rawTime()
						+ "): node "
						+ fId
						+ " accessed the cloud with query timestamp ["
						+ queryTimestamp
						+ "] got "
						+ ((updates == ICloud.NO_UPDATE) ? "NUP" : Arrays
								.toString(updates)));
			}

			if (updates == ICloud.NO_UPDATE) {
				fDisseminationService.post(new HFloodMMsg(clock.rawTime(), -1),
						engine);
			} else {
				// XXX note that we disseminate old timestamp information when
				// there's an update. Updates are therefore not so useful for
				// conveying freshness in this implementation.
				for (HFloodMMsg update : updates) {
					fDisseminationService.post(update, engine);
				}
			}
			
			/* XXX add back this check and fix the cases in which it fails.
 			if (clock.rawTime() - fLastAccess < fTimerPeriod) {
				throw new IllegalStateException();
			} */
			
			fLastAccess = clock.rawTime();

			// Our knowledge got more recent.
			updateKnowledge(fLastAccess, clock);
		}

		public void cancel() {
			fDone = true;
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
			return fDone;
		}

	}

}
