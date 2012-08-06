package it.unitn.disi.distsim.dataserver;

import java.util.Properties;

public class WorkUnit {
	public final Properties configuration;
	public final byte[] checkpoint;
	
	public WorkUnit(Properties configuration, byte[] checkpoint) {
		this.configuration = configuration;
		this.checkpoint = checkpoint;
	}
}
