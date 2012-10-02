package it.unitn.disi.distsim.dataserver;

import java.io.File;
import java.rmi.RemoteException;

import org.apache.log4j.Logger;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import it.unitn.disi.distsim.control.SimulationControl;

@XStreamAlias("checkpoint-manager")
public class CheckpointManager implements CheckpointManagerMBean {

	private static final Logger fLogger = Logger
			.getLogger(CheckpointManager.class);

	@XStreamAlias("sim-id")
	private String fSimId;

	@XStreamOmitField
	private volatile SimulationControl fParent;

	@XStreamOmitField
	private DataManagerImpl fManager;
	
	public CheckpointManager(String id) {
		this.fSimId = id;
	}

	@Override
	public synchronized void start() {
		DataManagerImpl mgr = new DataManagerImpl(checkpointFolder(),
				fParent.getConfigFolder(), fSimId);
		try {
			fParent.objectManager().publish(mgr,
					fParent.name(fSimId, "datamgr"));
		} catch (RemoteException ex) {
			fLogger.error("Failed to start checkpoint manager.", ex);
		}
	}

	private File checkpointFolder() {
		File chkpFolder = new File(fParent.getMasterOutputFolder(),
				"checkpoints");
		if (!chkpFolder.exists()) {
			chkpFolder.mkdir();
		}
		return chkpFolder;
	}

	@Override
	public synchronized void stop() {
		fParent.objectManager().remove(fParent.name(fSimId, "chkpmgr"));
	}

	@Override
	public synchronized boolean isRunning() {
		return fManager != null;
	}

	@Override
	public synchronized void setControl(SimulationControl parent) {
		fParent = parent;
	}

}
