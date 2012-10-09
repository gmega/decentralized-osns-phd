package it.unitn.disi.distsim.streamserver;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.BasicConfigurator;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

public class StreamServerCLI {
	
	@Option(name = "-p", aliases = { "--port" }, usage = "Listening port to server (defaults to 50327).", required = false)
	private int fPort = 50327;

	@Option(name = "-f", aliases = { "--folder" }, usage = "Folder where to store outputs.", required = true)
	private File fOutput;

	public static void main(String[] args) throws IOException {
		new StreamServerCLI()._main(args);
	}

	private void _main(String[] args) {
		CmdLineParser parser = new CmdLineParser(this);
		try {
			parser.parseArgument(args);
		} catch (CmdLineException ex) {
			System.err.println(ex.getMessage() + ".");
			parser.printUsage(System.err);
			System.exit(-1);
		}

		configureLogging();

		if (!fOutput.isDirectory()) {
			System.err.println(fOutput + "is not a valid output folder.");
			System.exit(-1);
		}

		StreamServerImpl server = new StreamServerImpl(fPort, fOutput);
		server.run();

	}

	private void configureLogging() {
		BasicConfigurator.configure();
	}
	
}
