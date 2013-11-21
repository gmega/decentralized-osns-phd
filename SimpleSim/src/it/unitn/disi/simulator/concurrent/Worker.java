package it.unitn.disi.simulator.concurrent;

import java.io.Serializable;
import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import it.unitn.disi.distsim.control.ControlClient;
import it.unitn.disi.distsim.dataserver.CheckpointClient;
import it.unitn.disi.distsim.dataserver.CheckpointClient.Application;
import it.unitn.disi.distsim.scheduler.ISchedulerClient;
import it.unitn.disi.distsim.scheduler.IWorker;
import it.unitn.disi.distsim.scheduler.RemoteSchedulerClient;
import it.unitn.disi.distsim.scheduler.generators.IScheduleIterator;
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.collections.Pair;
import it.unitn.disi.utils.logging.ProgressTracker;
import peersim.config.Attribute;
import peersim.config.IResolver;
import peersim.config.ObjectCreator;

public abstract class Worker implements Runnable, Application, IWorker {

	public static final String PROP_TASK_DATA = "task.data";

	public static final String PROP_TASK_STATS = "task.stats";

	public static final String PROP_TASK_PROGRESS = "task.progress";

	public static final String PROP_ACTIVE_TASKS = "task.active";

	private static final int ACTIVE_POLLING_INTERVAL = 10000;

	private static final int ONE_SECOND = 1000;

	private static final int ONE_MINUTE = 60 * ONE_SECOND;

	private static final Logger fLogger = Logger.getLogger(Worker.class);

	/**
	 * How many repetitions to run.
	 */
	@Attribute("repetitions")
	protected int fRepeat;

	/**
	 * How many cores to use.
	 */
	@Attribute("cores")
	protected int fCores;

	private final Lock fMutex;

	private volatile SimulationState fState;

	private final Thread fCheckpoint;

	private final ControlClient fControl;

	private final Properties fStatus;

	private int fFailed;

	private int fCompleted;

	/**
	 * The iterator which will feed us experiments.
	 */
	private final ISchedulerClient fClient;

	/**
	 * Client to the checkpoint manager.
	 */
	private final CheckpointClient fChkpClient;

	/**
	 * Executor for taking advantage of multicore processors.
	 */
	private TaskExecutor fExecutor;

	public Worker(
			@Attribute(Attribute.AUTO) IResolver resolver,
			@Attribute(value = "debug", defaultValue = "false") boolean debug,
			@Attribute(value = "local_schedule", defaultValue = "false") boolean localSchedule) {
		fMutex = new ReentrantLock();

		// Worker with coordinator.
		if (!localSchedule) {
			fLogger.info("Worker requires task coordinator.");
			fControl = ObjectCreator.createInstance(ControlClient.class, "",
					resolver);

			try {
				fChkpClient = new CheckpointClient(fControl, this,
						30 * ONE_MINUTE);
				fCheckpoint = new Thread(fChkpClient,
						"Application checkpoint thread");

				if (debug) {
					enableDebugging(resolver);
				}

			} catch (Exception ex) {
				throw MiscUtils.nestRuntimeException(ex);
			}

			fClient = new RemoteSchedulerClient(fControl, this);
		}

		// Independent worker.
		else {
			fLogger.info("Worker doesn't require task coordinator.");

			fControl = null;
			fCheckpoint = null;
			fChkpClient = null;

			fClient = ObjectCreator.createInstance(LocalScheduler.class, "",
					resolver);
		}

		fStatus = new Properties();
	}

	@Override
	public void run() {
		fExecutor = new TaskExecutor(fCores, fCores + 1);
		fLogger.info("Using " + fCores + " cores.");
		try {
			initialize();
			startCheckpointing();
			this.run0();
		} catch (Exception ex) {
			fLogger.error("Error during worker execution.", ex);
			// TODO add worker-master error reporting code here.
		} finally {
			stopCheckpointing();
		}
	}

	private void run0() throws Exception {
		IScheduleIterator schedule = schedule();
		Integer row;
		while ((row = (Integer) schedule.nextIfAvailable()) != IScheduleIterator.DONE) {
			Serializable experimentData = load(row);
			Object results = runTasks(row, experimentData);
			outputResults(results);
			taskDone();
		}
	}

	private IScheduleIterator schedule() {
		return fClient.iterator();
	}

	private Object runTasks(final int id, final Serializable data)
			throws Exception {

		startTask(id, data);

		final int iterations = fRepeat - fState.iteration;

		fExecutor.start(quiet(id, data) ? null : label(id, data), iterations);

		// Publishes the current progress of this worker as a property that
		// can be consumed by remote clients.
		StatusTracker tracker = new StatusTracker(taskTitle(data), iterations);
		tracker.startTask();

		// Thread that submits jobs.
		Thread submitter = new Thread(new Runnable() {
			@Override
			public void run() {
				for (int i = 0; i < iterations && !Thread.interrupted(); i++) {
					try {
						SimulationTask sTask = createTask(id, data);
						fExecutor.submit(sTask);
					} catch (InterruptedException e) {
						break;
					} catch (IllegalStateException e) {
						// We might get this when the batch is suspended.
						break;
					} catch (Exception ex) {
						fLogger.error(
								"Problem creating/submitting task. Stopping"
										+ " submission thread.", ex);
						fExecutor.cancelBatchWithException(ex);
					}
				}
			}
		}, "task submitter");

		submitter.start();

		// Consumes results as they become available.
		for (int i = 0; i < iterations; i++) {
			SimulationTask task = getTask(fExecutor);
			tracker.tick();
			if (task == null) {
				continue;
			}

			// Locks the aggregate state so that it cannot be checkpointed
			// while we change it.
			fMutex.lock();
			aggregate(fState.aggregate, i, task);

			// If the result aggregate is "precise" enough, stop running
			// experiments.
			if (isPreciseEnough(fState.aggregate)) {
				submitter.interrupt();
				fMutex.unlock();
				fExecutor.cancelBatch();
				try {
					submitter.join();
				} catch (InterruptedException e) {
					// Don't care.
				}
				break;
			}

			fState.iteration++;
			fMutex.unlock();
			updateTaskStatistics();
		}

		fCompleted++;
		tracker.done();

		return fState.aggregate;
	}

	private void stopCheckpointing() {
		if (fCheckpoint != null) {
			fCheckpoint.interrupt();
		}
	}

	private void startCheckpointing() {
		if (fCheckpoint != null) {
			fCheckpoint.start();
		}
	}

	private void updateTaskStatistics() {
		StringBuffer sbuffer = new StringBuffer();
		sbuffer.append("[");
		sbuffer.append("fail: ");
		sbuffer.append(fFailed);
		sbuffer.append(", complete: ");
		sbuffer.append(fCompleted);
		sbuffer.append("]");
		updateStatus(PROP_TASK_STATS, sbuffer.toString());
	}

	private SimulationTask getTask(TaskExecutor executor) throws Exception {
		Object value;
		try {
			value = executor.consume();
		} catch (InterruptedException ex) {
			return null;
		}

		if (value instanceof SimulationTask) {
			return (SimulationTask) value;
		}

		if (value instanceof SimulationTaskException) {
			SimulationTaskException ste = (SimulationTaskException) value;
			ste.dumpProperties(System.err);
			fFailed++;
			updateTaskStatistics();
		}

		if (value instanceof Exception) {
			throw (Exception) value;
		}

		System.err.println("Can't handle return value - " + value.toString());

		return null;
	}

	private void startTask(int id, Object data) {
		fMutex.lock();

		Object chkp = fChkpClient != null ? fChkpClient.workUnit(id, "") : null;

		if (chkp != null) {
			fLogger.info("Retrieved checkpoint for task " + id + ".");
			fState = (SimulationState) chkp;
		} else {
			fLogger.info("No checkpoint for task " + id + ".");
			fState = new SimulationState();
			fState.taskId = id;
			fState.iteration = 0;
			fState.aggregate = resultAggregate(id, data);
		}

		fMutex.unlock();

		updateTaskStatistics();
	}

	private void taskDone() {
		fMutex.lock();
		if (fChkpClient != null) {
			fChkpClient.taskDone(fState.taskId);
		}
		fState = null;
		fMutex.unlock();
	}

	private synchronized void updateStatus(String key, String value) {
		fStatus.setProperty(key, value);
	}

	private synchronized Properties statusCopy() {
		return (Properties) fStatus.clone();
	}

	private void enableDebugging(IResolver resolver) {
		fLogger.info("Debugging enabled.");

		Thread activeTaskMonitor = new Thread("ATMon") {
			@Override
			public void run() {
				try {
					while (!Thread.interrupted()) {
						synchronized (this) {
							wait(ACTIVE_POLLING_INTERVAL);
						}

						int tasks = fExecutor.activeTasks();
						fLogger.info("Active tasks: [" + tasks + "]");
						updateStatus(PROP_ACTIVE_TASKS, Integer.toString(tasks));
					}
				} catch (InterruptedException ex) {
					// Done.
				}
			}
		};

		activeTaskMonitor.setDaemon(true);
		activeTaskMonitor.start();
	}

	public void checkpointStart() {
		fMutex.lock();
	}

	public Pair<Integer, Serializable> state() {
		if (fState != null) {
			return new Pair<Integer, Serializable>(fState.taskId, fState);
		}
		return null;
	}

	public void checkpointEnd() {
		fMutex.unlock();
	}

	private static class SimulationState implements Serializable {
		private static final long serialVersionUID = 1L;

		public volatile int taskId;
		public volatile int iteration;

		public volatile Object aggregate;
	}

	private class StatusTracker extends ProgressTracker {

		StatusTracker(String taskTitle, int totalTicks) {
			super(taskTitle, totalTicks);
		}

		@Override
		protected void reportProgress(double percentage) {
			updateStatus(PROP_TASK_PROGRESS, percentage + "%");
		}

		@Override
		protected void disposeWidget() {
			updateStatus(PROP_TASK_PROGRESS, "[done]");
		}

		@Override
		protected void displayWidget() {
			updateStatus(PROP_TASK_DATA, title());
			updateStatus(PROP_TASK_PROGRESS, "0%");
		}

	}

	// -------------------------------------------------------------------------
	// IWorker interface.
	// -------------------------------------------------------------------------

	@Override
	public void echo() {

	}

	@Override
	public synchronized Properties status() {
		return statusCopy();
	}

	// -------------------------------------------------------------------------
	// Methods to be implemented by subclasses.
	// -------------------------------------------------------------------------

	/**
	 * Called before anything gets done.
	 */
	protected abstract void initialize() throws Exception;

	/**
	 * Loads the data corresponding to a given experiment.
	 * 
	 * @param row
	 *            experiment id.
	 * @return the data, or handle to data required by the experiment.
	 */
	protected abstract Serializable load(Integer row) throws Exception;

	/**
	 * Creates a {@link SimulationTask} for a given experiment.
	 * 
	 * @param id
	 *            experiment id.
	 * @param data
	 *            data returned by {@link #load(Integer)}.
	 * 
	 * @return a {@link SimulationTask} ready to be executed.
	 */
	protected abstract SimulationTask createTask(int id, Serializable data)
			throws Exception;

	/**
	 * Creates a new result aggregate, which will aggregate results for all
	 * repetitions for a given simulation. Aggregates are part of the
	 * checkpoints, and must be therefore {@link Serializable}.
	 * 
	 * @param id
	 * @return
	 */
	protected abstract Serializable resultAggregate(int id, Object data);

	protected abstract void aggregate(Object aggregate, int i,
			SimulationTask task);

	/**
	 * @param aggregate
	 * @return <code>true</code> if the estimator is precise enough and
	 *         simulations should stop, or <code>false</code> otherwise.
	 */
	protected abstract boolean isPreciseEnough(Object aggregate);

	protected abstract void outputResults(Object results);

	protected abstract String taskTitle(Serializable data);

	/**
	 * Provides a label to the current task, to be displayed by the progress
	 * tracker.
	 * 
	 * @param id
	 *            scheduler id of the task.
	 * @param data
	 *            object returned by {@link #load(Integer)}.
	 * @return a string label to be printed on screen.
	 */
	protected String label(int id, Object data) {
		StringBuffer lab = new StringBuffer();
		lab.append("Task: ");
		lab.append(id);
		lab.append(", ");
		lab.append(data.toString());

		return lab.toString();
	}

	/**
	 * Controls whether a task requires progress tracking or not.
	 * 
	 * @param id
	 *            scheduler id of the task.
	 * @param data
	 *            object returned by {@link #load(Integer)}.
	 * @return <code>true</code> if progress tracking is required, o
	 *         <code>false</code> otherwise.
	 */
	protected boolean quiet(int id, Serializable data) {
		return false;
	}

}
