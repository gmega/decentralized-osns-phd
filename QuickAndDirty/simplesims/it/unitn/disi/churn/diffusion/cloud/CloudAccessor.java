package it.unitn.disi.churn.diffusion.cloud;

import java.util.Arrays;
import java.util.Random;

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

	private static final long serialVersionUID = 7135912692487883268L;

	private static final boolean DEBUG = false;

	private final IDisseminationService fDisseminationService;

	private final ICloud fCloud;

	private final ISimulationEngine fSim;

	private final double fTimerPeriod;

	private final int fId;

	private CloudAccess fAccessor;

	private final Random fRandom;

	private double fLastHeard;

	private double fRandomizedTimer;

	private double fLoginGrace;

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
			double period, double delay, double loginGrace, int id,
			Random random) {
		fDisseminationService = disseminationService;
		fSim = sim;
		fCloud = cloud;
		fId = id;
		fTimerPeriod = period;
		fRandom = random;
		fLoginGrace = loginGrace;
		fRandomizedTimer = fTimerPeriod + delay;
	}

	@Override
	public void eventPerformed(ISimulationEngine state,
			Schedulable schedulable, double nextShift) {

		State pState = ((IProcess) schedulable).state();
		IClockData clock = state.clock();

		// If our node is going down, we have nothing to do.
		if (pState == State.down) {
			return;
		}

		// If logged in with timer already expired, add a little "grace period"
		// to give some opportunity to the gossip protocols to disseminate stuff
		// before going for cloud.
		if (nextAccess() < clock.rawTime()) {
			// If adding the delta would cross the session boundary, don't
			// bother. The session is probably too short to be worth it anyway.
			if (nextShift > (clock.rawTime() + fLoginGrace)) {
				// Otherwise, nudge.
				fRandomizedTimer = (clock.rawTime() - fLastHeard) + fLoginGrace;
				if (DEBUG) {
					printEvent("NUDGE", fLastHeard, clock.rawTime(),
							nextAccess());
				}
			} else if (DEBUG) {
				printEvent("NUDGE_ABORT", clock.rawTime(), nextAccess());
			}
		}

		scheduleCloudAccess(state.clock());
	}

	private void newTimer(IClockData clock, boolean fixed) {
		fLastHeard = clock.rawTime();
		fRandomizedTimer = nextTimer(fixed);
		if (DEBUG) {
			printEvent("NEWTIMER", clock.rawTime(), nextAccess());
		}
		scheduleCloudAccess(clock);
	}

	private double nextTimer(boolean noRandom) {
		return (fRandom == null || noRandom) ? fTimerPeriod : fRandom
				.nextDouble() * fTimerPeriod * 2;
	}

	private void scheduleCloudAccess(IClockData clock) {
		// We schedule the accessor if the next access is to
		// happen during the current session.
		boolean access = nextAccess() < process().time();

		// Yet, it could be that:
		// 1. we had a scheduled access, but got information regarding
		// recent updates. In this case:
		if (scheduled()) {
			// 1a. if the access state changed (i.e. a message came in
			// from someone telling us they know a very recent update)
			// to a point in which the next access has moved beyond the
			// current session, we just postpone it.
			if (!access) {
				if (DEBUG) {
					printEvent("ACCESS_QUENCHED", clock.rawTime(), nextAccess());
				}
				fAccessor.cancel();
			}

			// 1b. Otherwise, we just nudge it by the amount required.
			else if (nextAccess() != fAccessor.time()) {
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
		fAccessor = new CloudAccess(Math.max(clock.rawTime(), nextAccess()));
		fSim.schedule(fAccessor);
	}

	@Override
	public void messageReceived(IProcess process, HFloodMMsg message,
			IClockData clock, boolean duplicate) {

		// Duplicates are not important.
		if (duplicate) {
			return;
		}

		// Sanity check.
		if (process.state() == State.down) {
			throw new IllegalStateException(
					"Can't receive message while offline");
		}

		if (DEBUG) {
			printEvent("MESSAGE_RECEIVED", clock.rawTime(),
					"[" + message.toString() + "]", nextAccess());
		}

		// NUP is old, just leave it.
		if (message.timestamp() < fLastHeard) {
			return;
		}

		newTimer(clock, true);
	}

	private void printEvent(String eid, Object... stuff) {
		StringBuffer buffer = new StringBuffer();
		buffer.append("CA:");
		buffer.append(eid);
		buffer.append(" ");
		buffer.append(fId);
		buffer.append(" ");

		for (int i = 0; i < stuff.length; i++) {
			buffer.append(stuff[i].toString());
			buffer.append(" ");
		}

		buffer.deleteCharAt(buffer.length() - 1);
		System.err.println(buffer);
	}

	private double nextAccess() {
		return fLastHeard + fRandomizedTimer;
	}

	private IProcess process() {
		return fSim.network().process(fId);
	}

	@Override
	public boolean isDone() {
		return false;
	}

	private boolean scheduled() {
		return fAccessor != null && !fAccessor.isExpired();
	}

	private class CloudAccess extends Schedulable {

		private static final long serialVersionUID = -8609040165590560819L;

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
				throw new IllegalStateException(
						"A node that is down cannot access the cloud.");
			}

			fDone = true;
			IClockData clock = engine.clock();

			// Fetch updates.
			HFloodMMsg[] updates = fCloud.fetchUpdates(fId, -1, fLastHeard,
					engine);
			// System.err.println("Cloud access.");
			if (DEBUG) {
				printEvent(
						"CLOUD_ACCESS",
						clock.rawTime(),
						fLastHeard,
						((updates == ICloud.NO_UPDATE) ? "NUP" : Arrays
								.toString(updates)));
			}

			if (updates == ICloud.NO_UPDATE) {
				fDisseminationService.post(new HFloodMMsg(clock.rawTime(), -1),
						engine);
			} else {
				// XXX note that we disseminate old timestamp information when
				// there's an updlate. Updates are therefore not so useful for
				// conveying freshness in this implementation.
				for (HFloodMMsg update : updates) {
					fDisseminationService.post(update, engine);
				}
			}

			// Our knowledge got more recent.
			newTimer(clock, true);
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
