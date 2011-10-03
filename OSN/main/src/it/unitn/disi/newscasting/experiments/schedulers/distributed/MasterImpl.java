package it.unitn.disi.newscasting.experiments.schedulers.distributed;

import it.unitn.disi.newscasting.experiments.schedulers.ISchedule;
import it.unitn.disi.newscasting.experiments.schedulers.IScheduleIterator;
import it.unitn.disi.utils.collections.Pair;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

public class MasterImpl implements IMaster {

	private static final Logger fLogger = Logger.getLogger(MasterImpl.class);

	private static final Pair<Integer, Integer> ALL_DONE = new Pair<Integer, Integer>(-1, 0);

	private final AtomicInteger fId = new AtomicInteger(0);

	private final ConcurrentHashMap<Integer, WorkerEntry> fWorkers = new ConcurrentHashMap<Integer, WorkerEntry>();

	private final ExperimentEntry[] fExperiments;

	private volatile int fRemaining;

	public MasterImpl(ISchedule schedule) {
		fExperiments = new ExperimentEntry[schedule.size()];
		fRemaining = schedule.size();
		IScheduleIterator iterator = schedule.iterator();
		synchronized (fExperiments) {
			for (int i = 0; i < fExperiments.length; i++) {
				fExperiments[i] = new ExperimentEntry(
						iterator.nextIfAvailable());
			}
			Arrays.sort(fExperiments);
		}
	}

	@Override
	public int registerWorker(IWorker worker) {
		int id = fId.incrementAndGet();
		fWorkers.put(id, new WorkerEntry(worker, id));
		return id;
	}

	@Override
	public Pair<Integer, Integer> acquireExperiment(int workerId)
			throws RemoteException {
		Pair<Integer, Integer> acquired = null;
		synchronized (fExperiments) {
			while ((acquired = tryAcquire(workerId)) == null && fRemaining > 0) {
				try {
					fLogger.info("Worker " + workerId + " is waiting for jobs.");
					fExperiments.wait();
				} catch (InterruptedException ex) {
					fLogger.warn("Thread interrupted.", ex);
					break;
				}
			}
			
			if (fRemaining == 0) {
				fLogger.info("Worker " + workerId + " has no more jobs to run.");
				acquired = ALL_DONE;
			} else {
				fLogger.info("Worker " + workerId + " assigned to job "
						+ acquired.a + ".");
			}

			return acquired;
		}
	}

	protected Pair<Integer, Integer> tryAcquire(int workerId) {
		for (int i = 0; i < fExperiments.length; i++) {
			ExperimentEntry exp = fExperiments[i];
			if (exp.worker == null && !exp.done) {
				WorkerEntry entry = fWorkers.get(workerId);
				if (entry == null) {
					fLogger.warn("Worker " + workerId
							+ " died half way through assignment.");
				}
				fExperiments[i].worker = entry;
				return new Pair<Integer, Integer>(fExperiments[i].id, fRemaining);
			}
		}

		return null;
	}

	@Override
	public int remaining() throws RemoteException {
		return fRemaining;
	}

	@Override
	public void experimentDone(Integer experimentId) throws RemoteException {
		synchronized (fExperiments) {
			int idx = Arrays.binarySearch(fExperiments, experimentId);
			if (idx < 0 || idx >= fExperiments.length
					|| fExperiments[idx].id != experimentId) {
				throw new RemoteException("Invalid experiment ID "
						+ experimentId + ".");
			}
			
			ExperimentEntry entry = fExperiments[idx];
			
			fLogger.info("Worker " + entry.worker.id + " done with job "
					+ entry.id + ".");
			
			fExperiments[idx].done = true;
			fExperiments[idx].worker = null;
			fRemaining--;

			// No more experiments to run, wake up everyone who's
			// waiting.
			fExperiments.notifyAll();
		}
	}

	class WorkerEntry {
		final IWorker worker;
		final int id;

		public WorkerEntry(IWorker worker, int id) {
			this.worker = worker;
			this. id = id;
		}
	}

	class ExperimentEntry implements Comparable<Object> {
		final int id;
		WorkerEntry worker;
		boolean done;

		public ExperimentEntry(int id) {
			this.id = id;
		}

		@Override
		public int compareTo(Object o) {
			if (o instanceof ExperimentEntry) {
				return this.id - ((ExperimentEntry)o).id;
			} else if (o instanceof Integer) {
				return this.id - ((Integer)o);
			} else {
				throw new IllegalArgumentException();
			}
		}
	}

}
