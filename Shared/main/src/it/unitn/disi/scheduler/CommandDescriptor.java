package it.unitn.disi.scheduler;

import java.io.Serializable;

public class CommandDescriptor implements Serializable {

	private static final long serialVersionUID = -7339907366712441827L;

	public static final String NONE = "none";

	public final String[] command;

	public final String pwd;
	public final String input;
	public final String output;
	public final boolean compressOutput;

	public CommandDescriptor(String[] command, String input, String output,
			String pwd, boolean compressOutput) {
		this.command = command;
		this.input = input;
		this.output = output;
		this.pwd = pwd;
		this.compressOutput = compressOutput;
	}

	public String commandString() {
		StringBuffer sbuffer = new StringBuffer();
		for (String part : command) {
			sbuffer.append(part);
			sbuffer.append(" ");
		}

		if (sbuffer.length() > 0) {
			sbuffer.deleteCharAt(sbuffer.length() - 1);
		}

		return sbuffer.toString();
	}
}
