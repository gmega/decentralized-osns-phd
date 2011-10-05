package it.unitn.disi.newscasting.experiments.schedulers.distributed;

import it.unitn.disi.newscasting.experiments.schedulers.ISchedule;
import it.unitn.disi.newscasting.experiments.schedulers.IScheduleIterator;
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.collections.Pair;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import org.apache.log4j.Logger;

import peersim.config.Attribute;
import peersim.config.AutoConfig;

@AutoConfig
public class DistributedSchedulerClient implements ISchedule, IWorker {

	private static final Logger fLogger = Logger
			.getLogger(DistributedSchedulerClient.class);

	private final String fHost;
	
	private final String fQueueId;

	private final int fPort;
	
	private int fWorkerId;
	
	private Integer fInitialSize;

	public DistributedSchedulerClient(
			@Attribute("host") String host,
			@Attribute("queue") String queueId,
			@Attribute(value = "port", defaultValue = "50325") int port) {
		fHost = host;
		fPort = port;
		fQueueId = queueId;
	}

	@Override
	public IScheduleIterator iterator() {
		IWorker us = publish(); 
		IMaster master = connect();
		register(master, us);
		return new DistributedIterator(master);
	}

	private void register(IMaster master, IWorker worker) {
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

	private IMaster connect() {
		fLogger.info("Contacting master at " + fHost + ":" + fPort + ".");
		try {
			Registry registry = LocateRegistry.getRegistry(fHost, fPort);
			return (IMaster) registry.lookup(fQueueId);
		} catch (RemoteException ex) {
			fLogger.error("Failed to resolve registry at supplied address/port. "
					+ "Is the master instance running? Is the queue id right?");
			throw MiscUtils.nestRuntimeException(ex);
		} catch (NotBoundException ex) {
			fLogger.error("Master not bound under expected registry location "
					+ IMaster.MASTER_ADDRESS + ".");
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
