package it.unitn.disi.distsim.scheduler;

import it.unitn.disi.distsim.control.ControlClient;
import it.unitn.disi.distsim.scheduler.generators.ISchedule;
import it.unitn.disi.distsim.scheduler.generators.IScheduleIterator;
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.collections.Pair;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import org.apache.log4j.Logger;


public class SchedulerClient implements ISchedule, IWorker {

	private static final Logger fLogger = Logger
			.getLogger(SchedulerClient.class);

	private final ControlClient fClient;
	
	private int fWorkerId;
	
	private Integer fInitialSize;

	public SchedulerClient(ControlClient client) {
		fClient = client;
	}

	@Override
	public IScheduleIterator iterator() {
		IWorker us = publish(); 
		IScheduler master = connect();
		register(master, us);
		return new DistributedIterator(master);
	}

	private void register(IScheduler master, IWorker worker) {
		try {
			fLogger.info("Registering worker reference with master.");
			fInitialSize = master.remaining();
			fWorkerId = master.registerWorker(this);
		} catch (RemoteException ex) {
			fLogger.error("Failed to register reference.");
			throw MiscUtils.nestRuntimeException(ex);
		}
	}
	
	private IWorker publish() {
		try {
			fLogger.info("Publishing RMI reference.");
			return (IWorker) UnicastRemoteObject.exportObject(this, 0);
		} catch (RemoteException ex) {
			fLogger.error("Failed to publish object.");
			throw MiscUtils.nestRuntimeException(ex);
		}
	}

	private IScheduler connect() {
		try {
			return fClient.lookup("scheduler", IScheduler.class);
		} catch (RemoteException ex) {
			fLogger.error("Failed to resolve registry at supplied address/port. "
					+ "Is the master instance running? Is the queue id right?");
			throw MiscUtils.nestRuntimeException(ex);
		} 
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

		private final IScheduler fMaster;

		private int fRemaining;

		private int fPrevious = Integer.MIN_VALUE;

		protected DistributedIterator(IScheduler master) {
			fMaster = master;
		}

		@Override
		public Object nextIfAvailable() {
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
