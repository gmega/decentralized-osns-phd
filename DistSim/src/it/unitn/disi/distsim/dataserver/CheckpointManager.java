package it.unitn.disi.distsim.dataserver;

import it.unitn.disi.distsim.control.ISimulation;

import java.io.File;
import java.rmi.RemoteException;

import org.apache.log4j.Logger;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

@XStreamAlias("checkpoint-manager")
public class CheckpointManager implements CheckpointManagerMBean {

	private static final Logger fLogger = Logger
			.getLogger(CheckpointManager.class);

	@XStreamAlias("sim-id")
	private String fSimId;

	@XStreamAlias("autostart")
	private boolean fRunning;

	@XStreamOmitField
	private volatile ISimulation fSimulation;

	public CheckpointManager(String id, ISimulation simulation) {
		fSimId = id;
		fSimulation = simulation;
	}

	@Override
	public synchronized void start() {
		DataManagerImpl mgr = new DataManagerImpl(checkpointFolder(),
				fSimulation.baseFolder(), fSimId);
		try {
			fSimulation.publish("datamgr", mgr);
			fRunning = true;
		} catch (RemoteException ex) {
			fLogger.error("Failed to start checkpoint manager.", ex);
		}

		fSimulation.attributeListUpdated(this);
	}

	private File checkpointFolder() {
		File chkpFolder = new File(fSimulation.baseFolder(), "checkpoints");
		if (!chkpFolder.exists()) {
			chkpFolder.mkdir();
		}
		return chkpFolder;
	}

	@Override
	public synchronized void stop() {
		fSimulation.remove("chkpmgr");
		fRunning = false;
		fSimulation.attributeListUpdated(this);
	}

	@Override
	public synchronized boolean isRunning() {
		return fRunning;
	}

	@Override
	public synchronized void setSimulation(ISimulation simulation) {
		fSimulation = simulation;
	}

	@Override
	public boolean shouldAutoStart() {
		return fRunning;
	}

	@Override
	public void reset() {
		File chkpFolder = checkpointFolder();
		if (!chkpFolder.exists()) {
			return;
		}

		fLogger.info("Clearing checkpoints.");

		for (File checkpoint : chkpFolder.listFiles()) {
			if (checkpoint.getName().endsWith(
					"." + DataManagerImpl.CHK_EXTENSION)) {
				if (checkpoint.delete()) {
					fLogger.info("Checkpoint " + checkpoint.getName()
							+ " deleted.");
				} else {
					fLogger.error("Failed to delete " + checkpoint.getName()
							+ ".");
				}
			}
		}
	}

}
