package it.unitn.disi.newscasting.experiments.schedulers.distributed;

import it.unitn.disi.newscasting.experiments.schedulers.IntervalScheduler;
import it.unitn.disi.utils.tabular.TableReader;
import it.unitn.disi.utils.tabular.TableWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.apache.log4j.BasicConfigurator;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

public class MasterCLI {

	@Option(name = "-i", aliases = { "--input" }, usage = "Log file for recovery (input).", required = false)
	private File fInputLog;

	@Option(name = "-o", aliases = "--output", usage = "Log file for recovery (output). If ommitted, assumed to be equal to the input", required = false)
	private File fOutputLog = null;

	@Option(name = "-p", aliases = { "--port" }, usage = "Listening port to server (defaults to 50325).", required = false)
	private int fPort = 50325;

	@Option(name = "-r", aliases = { "--reuse" }, usage = "Reuses an already-started registry instance.", required = false)
	boolean fReuse = false;

	@Option(name = "-i", aliases = { "--idlist" }, usage = "Semicolon-separated list of comma-separated IDs.", required = true)
	private String fIdList;

	@Option(name = "-q", aliases = { "--queue" }, usage = "Name identifying this queue.", required = true)
	private String fQueueId;

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

		IntervalScheduler scheduler = new IntervalScheduler(fIdList);
		TableReader reader = fInputLog == null ? null : new TableReader(
				new FileInputStream(fInputLog));
		MasterImpl master = new MasterImpl(scheduler, reader);
		reader.close();

		if (fOutputLog == null) {
			if (fInputLog == null) {
				System.err.println("Error: either input or output logs "
						+ "(or both) must be specified.");
				parser.printUsage(System.err);
				System.exit(-1);
			} else {
				fInputLog = fOutputLog;
			}
		}

		master.start(fQueueId, !fReuse, fPort, new TableWriter(new PrintStream(
				fOutputLog), "experiment", "status"));
	}

	private void configureLogging() {
		BasicConfigurator.configure();
	}

	public static void main(String[] args) throws IOException {
		new MasterCLI()._main(args);
	}
}
