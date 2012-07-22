package it.unitn.disi.churn.diffusion.cloud;

import it.unitn.disi.churn.diffusion.IDisseminationService;
import it.unitn.disi.churn.diffusion.IMessageObserver;
import it.unitn.disi.churn.diffusion.Message;
import it.unitn.disi.simulator.core.EDSimulationEngine;
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

	private final IDisseminationService fDisseminationService;

	private final ICloud fCloud;

	private final EDSimulationEngine fSim;

	private final double fTimerPeriod;

	private final int fId;

	private CloudAccess fAccessor;

	private double fNextAccess;

	private double fNextShift;

	public CloudAccessor(EDSimulationEngine sim,
			IDisseminationService disseminationService, ICloud cloud,
			double period, int id) {
		fDisseminationService = disseminationService;
		fSim = sim;
		fCloud = cloud;
		fId = id;
		fTimerPeriod = period;
		fNextAccess = fTimerPeriod;
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
		scheduleCloudAccess(nextShift);
	}

	private void scheduleCloudAccess(double nextShift) {
		double accessTime = nextSchedule(fSim.clock().rawTime(), nextShift);
		if (scheduled() && accessTime < 0) {
			fAccessor.cancel();
		}

		if (!scheduled() && accessTime > 0) {
			fAccessor = new CloudAccess(accessTime);
			fSim.schedule(fAccessor);
		}
	}

	@Override
	public void messageReceived(Message message, IClockData clock) {
		// Sanity check.
		if (fNextShift < 0) {
			throw new IllegalStateException(
					"Can't receive message while offline");
		}

		fNextAccess = Math.max(fNextAccess, message.timestamp() + fTimerPeriod);
		scheduleCloudAccess(fNextShift);
	}

	private double nextSchedule(double time, double next) {
		// Has the timer expired while we were logged off?
		if (fNextAccess < time) {
			// Yes, so we access the cloud immediately.
			return 0.0;
		}
		// Will it expire now?
		else if (fNextAccess < next) {
			// Yes, schedule for the future.
			return next - fNextAccess;
		}

		// Nothing to do for now.
		return -1;
	}

	@Override
	public boolean isDone() {
		return false;
	}

	private boolean scheduled() {
		return fAccessor == null || fAccessor.isExpired();
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

			IClockData clock = engine.clock();

			// Fetch updates.
			Message[] updates = fCloud.fetchUpdates(fId, -1, fNextAccess
					- fTimerPeriod);

			if (updates != ICloud.NO_UPDATE) {
				fDisseminationService.post(new Message(clock.time(), fId),
						engine);
			} else {
				for (Message update : updates) {
					fDisseminationService.post(update, engine);
				}
			}

			fDone = true;
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
