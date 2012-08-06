package it.unitn.disi.distsim.dataserver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import org.apache.log4j.Logger;

import it.unitn.disi.distsim.control.ControlClient;
import it.unitn.disi.utils.collections.Pair;

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

	@Override
	public void run() {
		while (Thread.interrupted()) {
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
			oup = new ObjectOutputStream(buffer);
			Pair<Integer, Serializable> checkpoint = fApp.state();
			oup.writeObject(checkpoint.b);
			wid = checkpoint.a;
		} catch (IOException ex) {
			// Can't happen.
			throw new InternalError();
		}

		fManager.writeCheckpoint(wid, buffer.toByteArray());
	}

	public static interface Application {
		public void checkpointStart();
		public Pair<Integer, Serializable> state();
		public void checkpointEnd();
	}

}
