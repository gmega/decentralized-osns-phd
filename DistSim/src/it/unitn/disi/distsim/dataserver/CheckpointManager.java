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

	@XStreamAlias("autostart")
	private boolean fRunning;

	@XStreamOmitField
	private volatile SimulationControl fParent;

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
			fRunning = true;
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
		fParent.objectManager().remove(
				SimulationControl.name(fSimId, "chkpmgr"));
		fRunning = false;
	}

	@Override
	public synchronized boolean isRunning() {
		return fRunning;
	}

	@Override
	public synchronized void setControl(SimulationControl parent) {
		fParent = parent;
	}

	@Override
	public boolean shouldAutoStart() {
		return fRunning;
	}

}
