package it.unitn.disi.newscasting.experiments.schedulers.distributed;

import it.unitn.disi.newscasting.experiments.schedulers.ISchedule;
import it.unitn.disi.newscasting.experiments.schedulers.SchedulerFactory;
import it.unitn.disi.utils.HashMapResolver;
import it.unitn.disi.utils.tabular.TableReader;
import it.unitn.disi.utils.tabular.TableWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import peersim.config.IResolver;

public class MasterCLI {

	@Option(name = "-l", aliases = { "--replay" }, usage = "Log file for recovery (input).", required = false)
	private File fInputLog;

	@Option(name = "-o", aliases = "--output", usage = "Log file for recovery (output). If ommitted, the input log will be reused.", required = false)
	private File fOutputLog = null;

	@Option(name = "-p", aliases = { "--port" }, usage = "Listening port to server (defaults to 50325).", required = false)
	private int fPort = 50325;

	@Option(name = "-r", aliases = { "--reuse" }, usage = "Reuses an already-started registry instance.", required = false)
	boolean fReuse = false;

	@Option(name = "-q", aliases = { "--queue" }, usage = "Name identifying this queue.", required = true)
	private String fQueueId;

	@Option(name = "-m", aliases = { "--mode" }, usage = "Scheduling mode.", required = true)
	private String fMode;

	@Argument
	private List<String> fProperties = new ArrayList<String>();

	public void _main(String[] args) throws IOException {

		CmdLineParser parser = new CmdLineParser(this);
		try {
			parser.parseArgument(args);
		} catch (CmdLineException ex) {
			System.err.println(ex.getMessage() + ".");
			parser.printUsage(System.err);
			System.exit(-1);
		}

		configureLogging();

		IResolver resolver = resolver();
		TableReader reader = fInputLog == null ? null : new TableReader(
				new FileInputStream(fInputLog));
		ISchedule scheduler = SchedulerFactory.getInstance().createScheduler(
				resolver, null, null);
		MasterImpl master = new MasterImpl(scheduler, reader);

		if (reader != null) {
			reader.close();
		}

		boolean append = false;
		if (fOutputLog == null) {
			if (fInputLog == null) {
				System.err.println("Error: either input or output logs "
						+ "(or both) must be specified.");
				parser.printUsage(System.err);
				System.exit(-1);
			} else {
				fOutputLog = fInputLog;
				append = true;
			}
		}

		master.start(fQueueId, !fReuse, fPort, new TableWriter(new PrintWriter(
				new FileWriter(fOutputLog, append)), !append, "experiment", "status"));
	}

	private IResolver resolver() {
		HashMap<String, Object> pars = new HashMap<String, Object>();
		pars.put(IResolver.NULL_KEY, fMode);
		for (String property : fProperties) {
			int idx = property.indexOf('=');
			if (idx == -1) {
				pars.put(property, Boolean.toString(true));
			} else {
				pars.put(property.substring(0, idx),
						property.substring(idx + 1, property.length()));
			}
		}

		return new HashMapResolver(pars);
	}

	private void configureLogging() {
		BasicConfigurator.configure();
	}

	public static void main(String[] args) throws IOException {
		new MasterCLI()._main(args);
	}
}
