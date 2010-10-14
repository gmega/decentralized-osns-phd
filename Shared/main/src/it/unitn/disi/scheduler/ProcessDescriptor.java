package it.unitn.disi.scheduler;

import java.io.Serializable;

public class ProcessDescriptor implements Serializable {
	private static final long serialVersionUID = 7688143401967653702L;
	
	public final CommandDescriptor command;
	public final int pid;
	
	public ProcessDescriptor(CommandDescriptor command, int pid) {
		this.command = command;
		this.pid = pid;
	}
}
