package it.unitn.disi.distsim.dataserver;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

/**
 * The Command Line Interface companion to {@link GraphServerImpl}.
 * 
 * @author giuliano
 */
public class GraphServerCLI {
	
	@Option(name = "-i", aliases = { "--graphid" }, usage = "ID of the graph being served by this server", required = true)
	private String fGraphId;

	@Option(name = "-g", aliases = { "--graph" }, usage = "File containing the large graph.", required = true)
	private File fGraph;

	@Option(name = "-c", aliases = { "--catalog" }, usage = "File containing the graph's catalog.", required = true)
	private File fCatalog;

	@Option(name = "-p", aliases = { "--port" }, usage = "Listening port to server (defaults to 50326).", required = false)
	private int fPort = 50326;

	@Option(name = "-r", aliases = { "--reuse" }, usage = "Reuses an already-started registry instance.", required = false)
	boolean fReuse = false;

	public void _main(String[] args) {

		CmdLineParser parser = new CmdLineParser(this);
		try {
			parser.parseArgument(args);
		} catch (CmdLineException ex) {
			System.err.println(ex.getMessage() + ".");
			parser.printUsage(System.err);
			System.exit(-1);
		}
		
		configureLogging();
		
		Logger logger = Logger.getLogger(GraphServerCLI.class);
		
		try {
			GraphServerImpl server = new GraphServerImpl(fGraph, fCatalog);
			server.start(fGraphId, !fReuse, fPort);
		} catch (Exception ex) {
			logger.error("Failed to start graph server.", ex);
		}

	}
	
	private void configureLogging() {
		BasicConfigurator.configure();
	}
	
	public static void main(String[] args) throws IOException {
		new GraphServerCLI()._main(args);
	}
}
