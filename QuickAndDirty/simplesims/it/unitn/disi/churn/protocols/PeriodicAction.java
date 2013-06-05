package it.unitn.disi.churn.protocols;

import it.unitn.disi.simulator.core.IClockData;
import it.unitn.disi.simulator.core.IEventObserver;
import it.unitn.disi.simulator.core.IProcess;
import it.unitn.disi.simulator.core.IReference;
import it.unitn.disi.simulator.core.ISimulationEngine;
import it.unitn.disi.simulator.core.Schedulable;
import it.unitn.disi.simulator.core.IProcess.State;

/**
 * {@link PeriodicAction} implements, in an efficient way, a periodic action to
 * be performed by a node only if it is online. <BR>
 * <BR>
 * Such actions are performed every <b>delta</b> time instants, where delta is
 * taken w.r.t. the wallclock. If the timer expires while the node is offline,
 * then the action will be executed upon node login.<BR>
 * <BR>
 * {@link PeriodicAction} supports timer changes for scheduled actions.
 * 
 * @author giuliano
 */
public abstract class PeriodicAction implements IEventObserver {

	private static final long serialVersionUID = 7135912692487883268L;

	private static final double TIEBREAK_DELTA = 1.0/3600000000.0;

	private static final boolean DEBUG = false;

	private double fNextAccess;

	private int fTieBreaker;

	private int fId;

	private IReference<ISimulationEngine> fSim;

	private PeriodicSchedulable fAction;

	public PeriodicAction(IReference<ISimulationEngine> engine, int tieBreaker,
			int id, double initial) {
		fSim = engine;
		fTieBreaker = tieBreaker;
		fId = id;
		fNextAccess = initial + priorityPenalty();
		System.err.println(fNextAccess);
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

		// If logged in with timer already expired, schedule ourselves to
		// 'effectively immediately', together with a "priority penalty" (hack
		// to enforce deterministic ordering among tied PeriodicActions).
		if (fNextAccess < clock.rawTime()) {
			// In the unlikely event breaking ties would cross a session
			// boundary, don't even try to schedule, but emmit a warning.
			if (nextShift > (clock.rawTime() + priorityPenalty())) {
				// Otherwise, nudge.
				double oldAccess = fNextAccess;
				fNextAccess = clock.rawTime() + priorityPenalty();
				if (DEBUG) {
					printEvent("NUDGE", oldAccess, clock.rawTime(), fNextAccess);
				}
			} else {
				printEvent("NUDGE_ABORT", clock.rawTime(), fNextAccess);
			}
		}

		// Otherwise we schedule the access only if it is scheduled to happen
		// during the current session.
		if (shouldAccess()) {
			scheduleAction(clock);
		}
	}

	/**
	 * This method is called when the access time gets redefined. It is valid to
	 * call it only if the receiving node is online.
	 * 
	 * @param clock
	 * @param newAccessTime
	 */
	public void newTimer(IClockData clock, double newAccessTime) {
		// If the receiving process is not up, it cannot have its action time
		// readjusted.
		if (!process().isUp()) {
			throw new IllegalStateException("Timer cannot be changed for "
					+ "offline processes.");
		}

		// First, we update the access schedule to reflect the shift in
		// base.
		fNextAccess = newAccessTime;

		// Three cases are possible:

		// 1. We already had an access scheduled.
		if (scheduled()) {
			// 1a. and the access state changed to a point in which the next
			// access has moved beyond the current session, we just postpone it.
			if (!shouldAccess()) {
				if (DEBUG) {
					printEvent("QUENCH", clock.rawTime(), fNextAccess);
				}
				fAction.cancel();
			}

			// 1b. Otherwise, we just nudge it by the amount required.
			else if (fNextAccess != fAction.time()) {
				fAction.cancel();
				scheduleAction(clock);
			}

		}

		// 2. No accesses were scheduled, but the new access time puts the
		// next access inside of the current session.
		else if (shouldAccess()) {
			scheduleAction(clock);
		}

		// 3. Nothing was scheduled, and the next access still falls outside
		// of the current session. Nothing to do.
	}
	
	public int getPriority() {
		return fTieBreaker;
	}

	@Override
	public boolean isDone() {
		return false;
	}

	private boolean shouldAccess() {
		return fNextAccess < process().time();
	}

	private double priorityPenalty() {
		return fTieBreaker * TIEBREAK_DELTA;
	}

	private IProcess process() {
		return fSim.get().network().process(fId);
	}

	private void scheduleAction(IClockData clock) {
		fAction = new PeriodicSchedulable(fNextAccess);
		fSim.get().schedule(fAction);
	}

	private boolean scheduled() {
		return fAction != null && !fAction.isExpired();
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

	protected abstract double nextCycle(ISimulationEngine engine);

	protected abstract void performAction(ISimulationEngine engine);

	private class PeriodicSchedulable extends Schedulable {

		private static final long serialVersionUID = -8609040165590560819L;

		private final double fTime;

		private boolean fDone;

		private PeriodicSchedulable(double time) {
			fTime = time;
		}

		@Override
		public void scheduled(ISimulationEngine state) {
			if (fDone) {
				return;
			}

			if (!process().isUp()) {
				throw new IllegalStateException(
						"A node that is down cannot access the cloud.");
			}

			fDone = true;

			performAction(state);

			newTimer(state.clock(), nextCycle(state) + priorityPenalty());
		}

		@Override
		public double time() {
			return fTime;
		}

		@Override
		public boolean isExpired() {
			return fDone;
		}

		@Override
		public int type() {
			return Integer.MAX_VALUE;
		}

		public void cancel() {
			fDone = true;
		}

	}

}
