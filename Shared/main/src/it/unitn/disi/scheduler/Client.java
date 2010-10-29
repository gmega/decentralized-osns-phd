package it.unitn.disi.scheduler;

import java.rmi.RemoteException;
import java.rmi.server.ExportException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

public class Client {

	enum Actions {
		start, stop, killall, list, kill, submit
	}

	@Option(name = "-a", aliases = { "--action" }, usage = "One in [start | stop | list | kill | killall | submit].", required = true)
	private String fActionString;

	@Option(name = "-p", aliases = { "--port" }, usage = "Uses a specific port for communicating with the daemon (or launching it).", required = false)
	private int fPort = 0;

	@Option(name = "-c", aliases = { "--cores" }, usage = "Limits the number of cores that are to be used. If unspecified, uses all available.", required = false)
	private int fCores = 0;

	@Option(name = "-i", aliases = { "--input" }, usage = "Input file to direct to the standard input. Defauts to none.", required = false)
	private String fInput = CommandDescriptor.NONE;

	@Option(name = "-o", aliases = { "--output" }, usage = "Output file to connect to the standard output. Defaults to none.", required = false)
	private String fOutput = CommandDescriptor.NONE;

	@Option(name = "-w", aliases = { "--pwd" }, usage = "Working directory for the command. Defaults to the current directory.", required = false)
	private String fPwd = System.getenv("PWD");

	@Argument
	private List<String> fArguments = new ArrayList<String>();

	private int _main(String[] args) {
		CmdLineParser parser = new CmdLineParser(this);
		
		try {
			Actions action = parseArgument(args, parser);
			if (action == null) {
				printHelp(parser);
				System.exit(1);
			}
			
			ClientAPI api = new ClientAPI(fPort);
			// Checks that the daemon is running, for actions that require it.
			if (action != Actions.start && !checkRunning(api)) {
				return 1;
			}

			// Executes the action.
			switch (action) {

			case start:
				startDaemon(api);
				break;

			case stop:
				stopDaemon(api);
				break;

			case submit:
				if (fArguments.size() < 1) {
					System.err
							.println("Submit requires a command enclosed in double quotes (\").");
					break;
				}
				submit(api, fArguments.get(0).split(" "));
				break;

			case list:
				listProcesses(api);
				break;

			case kill:
				if (fArguments.size() < 1) {
					System.err
							.println("Kill action requires a process id. "
									+ "Use LIST to find the currently running processes.");
					break;
				}
				killProcess(api, fArguments.get(0));
				break;

			case killall:
				killAll(api);
				break;
			}

		} catch (CmdLineException ex) {
			System.err.println(ex.getMessage());
			printHelp(parser);
		} catch (RemoteException ex) {
			System.err
					.println("Something went wrong while executing your action.");
			System.err.println("Message was: " + ex.getMessage());
			return 2;
		}

		return 0;
	}

	private Actions parseArgument(String[] args, CmdLineParser parser)
			throws CmdLineException {
		parser.parseArgument(args);
		
		Actions action;
		try {
			action = Actions.valueOf(fActionString.toLowerCase());
		} catch (IllegalArgumentException ex) {
			System.err.println("Invalid action " + fActionString + ".");
			action = null;
		}
		return action;
	}

	private void submit(ClientAPI api, String [] command) {
		try {
			api.resolveObject().submit(
					new CommandDescriptor(command, fInput, fOutput, fPwd));
		} catch (RemoteException ex) {
			defaultMessage("Submission", ex);
		}
	}

	private void killAll(ClientAPI api) {
		try {
			api.resolveObject().killall();
		} catch (RemoteException ex) {
			defaultMessage("Killall", ex);
		}
	}

	private void killProcess(ClientAPI api, String string) {
		try {
			api.resolveObject().kill(Integer.parseInt(string));
		} catch (RemoteException ex) {
			defaultMessage("Process killing", ex);
		} catch (NumberFormatException ex) {
			System.err.println("Process ID must be a number.");
		}
	}

	private void listProcesses(ClientAPI api) {
		List<ProcessDescriptor> entries = null;
		try {
			entries = api.resolveObject().list();
		} catch (RemoteException ex) {
			defaultMessage("List acquisition", ex);
			return;
		}
		
		if (entries.size() == 0) {
			System.err.println("There are no running processes.");
			return;
		}

		System.err.println("Running processes:");

		for (ProcessDescriptor entry : entries) {
			System.err.println("[" + entry.pid + "] - " + entry.command.commandString());
		}
	}

	private void stopDaemon(ClientAPI api) {
		try {
			api.resolveObject().shutdown();
		} catch (RemoteException ex) {
			defaultMessage("Shutdown", ex);
		}
	}

	private void startDaemon(ClientAPI api) throws RemoteException {
		int cores = fCores > 0 ? fCores : Runtime.getRuntime()
				.availableProcessors();
		System.err.println("Server will use " + cores + " cores.");

		try {
			// Maybe I'll allow more flexible schemes at some point...
			BasicConfigurator.configure();
			api.start(cores);
		} catch (ExportException ex) {
			// Ah, the wonders of RMI exception decoding...
			if (ex.getMessage().equals("object already exported")) {
				System.err
						.println("It appears that the daemon is already running.");
			} else if (ex.getMessage().startsWith("Port already in use")) {
				System.err.println("Port " + fPort
						+ " seems to be already in use.");
			}
		}
	}

	private boolean checkRunning(ClientAPI api) {
		try {
			if (!api.isRunning()) {
				System.err
						.println("It appears that the daemon is not running.");
				return false;
			}
			return true;
		} catch (RemoteException ex) {
			System.err.println("Cannot determine if the daemon is running.");
			System.err.println("Message was: " + ex.getMessage());
			return false;
		}
	}

	private void defaultMessage(String op, RemoteException ex) {
		System.err.println(op + "failed. Message was: " + ex.getMessage());
	}

	private void printHelp(CmdLineParser parser) {
		System.err.println(this.getClass().getSimpleName() + " action");
		parser.printUsage(System.err);
		System.err.println();
	}

	public static void main(String[] args) {
		System.exit(new Client()._main(args));
	}

}
