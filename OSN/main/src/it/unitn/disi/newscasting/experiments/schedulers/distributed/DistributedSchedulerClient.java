package it.unitn.disi.newscasting.experiments.schedulers.distributed;

import it.unitn.disi.newscasting.experiments.schedulers.ISchedule;
import it.unitn.disi.newscasting.experiments.schedulers.IScheduleIterator;
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.collections.Pair;

import java.rmi.RemoteException;
import java.util.Iterator;

public class DistributedSchedulerClient implements ISchedule, IWorker {

	private IMaster fMaster;
	
	private int fWorkerId;
	
	private Integer fInitialSize;
	
	public DistributedSchedulerClient() {

	}

	@Override
	public IScheduleIterator iterator() {
//		IMaster master = connect();
		return null;
	}

	private IMaster connect() {
		
//		fInitialSize = fMaster.remaining();
//		fMaster.registerWorker(this);
		return null;
	}

	@Override
	public int size() {
		return fInitialSize;
	}

	@Override
	public void echo() throws RemoteException {
		// Does nothing.
	}

	class DistributedIterator implements IScheduleIterator {

		private final IMaster fMaster;

		private int fRemaining;
		
		private int fPrevious = Integer.MIN_VALUE;

		protected DistributedIterator(IMaster master) {
			fMaster = master;
		}

		@Override
		public Integer nextIfAvailable() {
			releasePrevious();
			Pair<Integer, Integer> next = acquire();
			refreshRemaining(next);
			if (fRemaining == 0) {
				return IScheduleIterator.DONE;
			}

			return experimentId(next);
		}

		@Override
		public int remaining() {
			return fRemaining;
		}

		private Integer experimentId(Pair<Integer, Integer> pair) {
			fPrevious = pair.a;
			return fPrevious;
		}

		private void refreshRemaining(Pair<Integer, Integer> pair) {
			fRemaining = pair.b;
		}
		
		private void releasePrevious() {
			if (fPrevious != Integer.MIN_VALUE) {
				try {
					fMaster.experimentDone(fPrevious);
				} catch (Exception ex) {
					throw MiscUtils.nestRuntimeException(ex);
				}
			}
		}

		private Pair<Integer, Integer> acquire() {
			try {
				return fMaster.acquireExperiment(fWorkerId);
			} catch (Exception ex) {
				throw MiscUtils.nestRuntimeException(ex);
			}
		}
	}

}
