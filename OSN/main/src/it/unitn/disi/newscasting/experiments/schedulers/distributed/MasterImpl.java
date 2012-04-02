package it.unitn.disi.newscasting.experiments.schedulers.distributed;

import it.unitn.disi.newscasting.experiments.schedulers.ISchedule;
import it.unitn.disi.newscasting.experiments.schedulers.IScheduleIterator;
import it.unitn.disi.utils.collections.Pair;
import it.unitn.disi.utils.tabular.TableReader;
import it.unitn.disi.utils.tabular.TableWriter;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

public class MasterImpl implements IMaster, IMasterAdmin, Runnable {

	private static enum ExperimentState {
		assigned, done;
	}

	private static final int POLLING_INTERVAL = 1000;

	private static final Logger fLogger = Logger.getLogger(MasterImpl.class);

	private static final Pair<Integer, Integer> ALL_DONE = new Pair<Integer, Integer>(
			-1, 0);

	private final AtomicInteger fId = new AtomicInteger(0);

	private final ConcurrentHashMap<Integer, WorkerEntry> fWorkers = new ConcurrentHashMap<Integer, WorkerEntry>();

	private final ExperimentEntry[] fExperiments;

	private final Thread fController;

	private volatile TableWriter fWriter;

	private volatile int fRemaining;

	public MasterImpl(ISchedule schedule, TableReader recover)
			throws IOException {
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

		if (recover != null) {
			replayLog(recover);
		}

		fController = new Thread(new WorkerControl(POLLING_INTERVAL));
	}

	public void start(String queueId, boolean createRegistry, int port,
			TableWriter log) {
		fWriter = log;
		fLogger.info("Starting registry and publishing object reference.");
		try {
			if (createRegistry) {
				LocateRegistry.createRegistry(port);
			}
			UnicastRemoteObject.exportObject(this, 0);
			Registry registry = LocateRegistry.getRegistry(port);
			registry.rebind(queueId, this);
		} catch (RemoteException ex) {
			fLogger.error("Error while publishing object.", ex);
			System.exit(-1);
		}
		Runtime.getRuntime().addShutdownHook(new Thread(this));
		fController.start();
		fLogger.info("All good.");
	}

	@Override
	public void run() {
		fController.interrupt();
		try {
			fController.join();
		} catch (InterruptedException ex) {
			// Swallows and returns.
		}
	}

	private void replayLog(TableReader log) throws IOException {
		fLogger.info("Replaying experiment log file...");
		int done = 0;
		while (log.hasNext()) {
			log.next();
			int id = Integer.parseInt(log.get("experiment"));
			switch (ExperimentState.valueOf(log.get("status"))) {
			case done:
				synchronized (fExperiments) {
					ExperimentEntry exp = experimentByID(id);
					if (exp == null) {
						fLogger.warn("Log entry points to non-existent experiment "
								+ id + ".");
						continue;
					}
					exp.done = true;
					done++;
				}
			}
		}

		fLogger.info("Log replay complete. " + done
				+ " experiments marked as done.");
	}

	private ExperimentEntry experimentByID(int id) {
		for (ExperimentEntry entry : fExperiments) {
			if (entry.id == id) {
				return entry;
			}
		}
		return null;
	}

	@Override
	public int registerWorker(IWorker worker) {
		int id = fId.incrementAndGet();
		String host = null;
		try {
			host = RemoteServer.getClientHost();
		} catch (ServerNotActiveException e) {
			fLogger.error("Error while querying for the client's host.", e);
		}
		fWorkers.put(id, new WorkerEntry(worker, host, id));
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
				fWriter.set("experiment", id(acquired));
				fWriter.set("status", ExperimentState.assigned);
				fWriter.emmitRow();
				fWriter.flush();
			}

			return acquired;
		}
	}

	protected void deadWorker(WorkerEntry entry) {
		fLogger.warn("Worker "
				+ entry.id
				+ " has died. Now checking if it had any experiments assigned to it.");
		fWorkers.remove(entry.id);
		synchronized (fExperiments) {
			for (ExperimentEntry exp : fExperiments) {
				if (exp.worker == entry) {
					exp.worker = null;
					fLogger.info("Job " + exp.id
							+ " has been returned to the pool.");
				}
			}
		}

	}

	protected Pair<Integer, Integer> tryAcquire(int workerId)
			throws RemoteException {
		WorkerEntry entry = fWorkers.get(workerId);
		if (entry == null) {
			throw new InvalidWorkerException("Worker " + workerId + " is not valid.");
		}

		for (int i = 0; i < fExperiments.length; i++) {
			ExperimentEntry exp = fExperiments[i];
			if (exp.worker == null && !exp.done) {
				fExperiments[i].worker = entry;
				return new Pair<Integer, Integer>(fExperiments[i].id,
						fRemaining);
			}
		}

		return null;
	}

	private int id(Pair<Integer, Integer> pair) {
		return pair.a;
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

			fWriter.set("experiment", entry.id);
			fWriter.set("status", ExperimentState.done);
			fWriter.emmitRow();
			fWriter.flush();

			fExperiments[idx].done = true;
			fExperiments[idx].worker = null;
			fRemaining--;

			// No more experiments to run, wake up everyone who's
			// waiting.
			fExperiments.notifyAll();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public Pair<String, Integer>[] registeredWorkers() {
		ArrayList<Pair<String, Integer>> workers = new ArrayList<Pair<String, Integer>>();
		for (Integer id : fWorkers.keySet()) {
			WorkerEntry entry = fWorkers.get(id);
			if (entry == null) {
				continue;
			}
			workers.add(new Pair<String, Integer>(entry.host, id));
		}
		return workers.toArray((Pair<String, Integer>[]) new Object[workers
				.size()]);
	}

	@Override
	public int total() {
		return fExperiments.length;
	}

	@Override
	public double completion() throws RemoteException {
		return ((double) remaining()) / ((double) total());
	}

	class WorkerEntry {

		final IWorker worker;

		final int id;

		final String host;

		public WorkerEntry(IWorker worker, String host, int id) {
			this.worker = worker;
			this.id = id;
			this.host = host;
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
				return this.id - ((ExperimentEntry) o).id;
			} else if (o instanceof Integer) {
				return this.id - ((Integer) o);
			} else {
				throw new IllegalArgumentException();
			}
		}
	}

	class WorkerControl implements Runnable {

		private final int fPollingInterval;

		public WorkerControl(int pollingInterval) {
			fPollingInterval = pollingInterval;
		}

		@Override
		public void run() {
			while (!Thread.interrupted()) {
				// Loosely pings all workers on the table. It's not
				// a problem if we lose some workers on a single run.
				for (WorkerEntry entry : fWorkers.values()) {
					try {
						entry.worker.echo();
					} catch (RemoteException ex) {
						// Worker presumed dead.
						// XXX maybe check exception type to see
						// if that's actually the case?
						deadWorker(entry);
					}
				}

				try {
					Thread.sleep(fPollingInterval);
				} catch (InterruptedException ex) {
					break;
				}
			}
		}
	}
}
