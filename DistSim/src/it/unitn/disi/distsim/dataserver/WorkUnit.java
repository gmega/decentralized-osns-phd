package it.unitn.disi.distsim.dataserver;

import java.io.Serializable;
import java.util.Properties;

public class WorkUnit implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	public final Properties configuration;
	public final byte[] checkpoint;

	public WorkUnit(Properties configuration, byte[] checkpoint) {
		this.configuration = configuration;
		this.checkpoint = checkpoint;
	}

	public boolean isNull() {
		return configuration == null && checkpoint == null;
	}
}
