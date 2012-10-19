package it.unitn.disi.distsim.dataserver;

import it.unitn.disi.distsim.control.ControlClient;
import it.unitn.disi.utils.collections.Pair;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import org.apache.log4j.Logger;

public class CheckpointClient implements Runnable {

	private static final Logger fLogger = Logger
			.getLogger(CheckpointClient.class);

	private final IDataManager fManager;

	private final Application fApp;

	private final long fChkpInterval;

	public CheckpointClient(ControlClient client, Application app,
			long chkpInterval) throws RemoteException, NotBoundException {
		fManager = client.lookup("datamgr", IDataManager.class);
		fChkpInterval = chkpInterval;
		fApp = app;
	}

	public Object workUnit(int wid, String wtype) {

		WorkUnit unit;
		try {
			unit = fManager.workUnit(wid, wtype);
			if (unit.isNull()) {
				return null;
			}
		} catch (RemoteException ex) {
			fLogger.error("Error retrieving checkpoint.", ex);
			return null;
		}

		ObjectInputStream iStream;

		try {
			iStream = new ObjectInputStream(new ByteArrayInputStream(
					unit.checkpoint));
			return iStream.readObject();
		} catch (Exception ex) {
			fLogger.error("Error de-serializing checkpoint.", ex);
			return null;
		}

	}

	public void taskDone(int wid) {
		try {
			fManager.clearCheckpoints(wid);
		} catch (RemoteException ex) {
			fLogger.error("Failed to clear checkpoint data for task " + wid
					+ ".", ex);
		}
	}

	@Override
	public void run() {
		while (!Thread.interrupted()) {
			try {
				Thread.sleep(fChkpInterval);
			} catch (InterruptedException ex) {
				// Done.
				break;
			}

			try {
				checkpoint();
			} catch (RemoteException ex) {
				fLogger.error("");
			}
		}
	}

	private void checkpoint() throws RemoteException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		ObjectOutputStream oup;

		int wid;

		try {
			fApp.checkpointStart();
			fLogger.info("Start checkpoint.");

			oup = new ObjectOutputStream(buffer);
			Pair<Integer, Serializable> checkpoint = fApp.state();
			if (checkpoint != null) {
				oup.writeObject(checkpoint.b);
				wid = checkpoint.a;
				fManager.writeCheckpoint(wid, buffer.toByteArray());
			}
		} catch (Exception ex) {
			fLogger.error("Error submitting checkpoint.", ex);
		} finally {
			fApp.checkpointEnd();
		}
		
		fLogger.info("Checkpoint done.");
	}

	public static interface Application {
		public void checkpointStart();

		public Pair<Integer, Serializable> state();

		public void checkpointEnd();
	}

}
