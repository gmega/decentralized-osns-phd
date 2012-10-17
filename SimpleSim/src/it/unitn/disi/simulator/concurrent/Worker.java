package it.unitn.disi.simulator.concurrent;

import java.io.Serializable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import it.unitn.disi.distsim.control.ControlClient;
import it.unitn.disi.distsim.dataserver.CheckpointClient;
import it.unitn.disi.distsim.dataserver.CheckpointClient.Application;
import it.unitn.disi.distsim.scheduler.SchedulerClient;
import it.unitn.disi.distsim.scheduler.generators.IScheduleIterator;
import it.unitn.disi.simulator.random.SimulationTaskException;
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.collections.Pair;
import peersim.config.Attribute;
import peersim.config.IResolver;
import peersim.config.ObjectCreator;

public abstract class Worker implements Runnable, Application {

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

	/**
	 * What burnin value to use.
	 */
	@Attribute("burnin")
	protected double fBurnin;

	private final Lock fMutex;

	private volatile SimulationState fState;

	private final Thread fCheckpoint;

	private final ControlClient fControl;

	/**
	 * The iterator which will feed us experiments.
	 */
	private final SchedulerClient fClient;

	/**
	 * Client to the checkpoint manager.
	 */
	private final CheckpointClient fChkpClient;

	/**
	 * Executor for taking advantage of multicore processors.
	 */
	private TaskExecutor fExecutor;

	public Worker(@Attribute(Attribute.AUTO) IResolver resolver) {
		fMutex = new ReentrantLock();

		fControl = ObjectCreator.createInstance(ControlClient.class, "",
				resolver);

		try {
			fChkpClient = new CheckpointClient(fControl, this, 30*ONE_MINUTE);
			fCheckpoint = new Thread(fChkpClient,
					"Application checkpoint thread");
		} catch (Exception ex) {
			throw MiscUtils.nestRuntimeException(ex);
		}

		fClient = new SchedulerClient(fControl);
	}

	@Override
	public void run() {
		fExecutor = new TaskExecutor(fCores, fCores + 1);
		try {
			initialize();
			fCheckpoint.start();
			this.run0();
		} catch (Exception ex) {
			fLogger.error(ex);
		} finally {
			fCheckpoint.interrupt();
		}
	}

	private void run0() throws Exception {
		IScheduleIterator schedule = fClient.iterator();
		Integer row;
		while ((row = (Integer) schedule.nextIfAvailable()) != IScheduleIterator.DONE) {
			Object experimentData = load(row);
			Object results = runTasks(row, experimentData);
			outputResults(results);
			taskDone();
		}
	}

	private Object runTasks(final int id, final Object data) {

		startTask(id, data);

		final int iterations = fRepeat - fState.iteration;

		fExecutor.start(label(id, data), iterations);

		Thread submitter = new Thread(new Runnable() {
			@Override
			public void run() {
				for (int i = 0; i < iterations; i++) {
					SimulationTask sTask = createTask(id, data);
					try {
						fExecutor.submit(sTask);
					} catch (InterruptedException e) {
						break;
					}
				}
			}
		}, "task submitter");

		submitter.start();

		for (int i = 0; i < iterations; i++) {
			SimulationTask task = getTask(fExecutor);
			if (task == null) {
				continue;
			}
			fMutex.lock();
			aggregate(fState.aggregate, i, task);
			if (isPreciseEnough(fState.aggregate)) {
				submitter.interrupt();
				fMutex.unlock();
				fExecutor.cancelBatch();
				break;
			}
			fState.iteration++;
			fMutex.unlock();
		}

		return fState.aggregate;
	}

	private SimulationTask getTask(TaskExecutor executor) {
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
		} else {
			System.err.println("Can't handle return value - "
					+ value.toString());
		}

		return null;
	}

	protected String label(int id, Object data) {
		StringBuffer lab = new StringBuffer();
		lab.append("Task: ");
		lab.append(id);
		lab.append(", ");
		lab.append(data.toString());

		return lab.toString();
	}

	private void startTask(int id, Object data) {
		fMutex.lock();

		Object chkp = fChkpClient.workUnit(id, "");

		if (chkp != null) {
			fState = (SimulationState) chkp;
		} else {
			fState = new SimulationState();
			fState.taskId = id;
			fState.iteration = 0;
			fState.aggregate = resultAggregate(id, data);
		}

		fMutex.unlock();
	}

	private void taskDone() {
		fMutex.lock();
		fChkpClient.taskDone(fState.taskId);
		fState = null;
		fMutex.unlock();
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
	protected abstract Object load(Integer row) throws Exception;

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
	protected abstract SimulationTask createTask(int id, Object data);

	/**
	 * Creates a new result aggregate, which will aggregate results for all
	 * repetitions for a given simulation. Aggregates are part of the
	 * checkpoints, and must be therefore {@link Serializable}.
	 * 
	 * @param id
	 * @return
	 */
	protected abstract Serializable resultAggregate(int id, Object data);

	/**
	 * @param aggregate
	 * @return <code>true</code> if the estimator is precise enough and
	 *         simulations should stop, or <code>false</code> otherwise.
	 */
	protected abstract boolean isPreciseEnough(Object aggregate);

	protected abstract void aggregate(Object aggregate, int i,
			SimulationTask task);

	protected abstract void outputResults(Object results);

}
