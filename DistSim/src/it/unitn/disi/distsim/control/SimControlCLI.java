package it.unitn.disi.distsim.control;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.URL;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import peersim.config.AutoConfig;

@AutoConfig
public class SimControlCLI {
	
	@Option(name = "-f", aliases = { "--folder" }, usage = "Master folder for this controller.", required = true)
	private File fMasterFolder;

	@Option(name = "-p", aliases = { "--port" }, usage = "Listening port for RMI registry (defaults to 50325).", required = false)
	private int fPort = 50325;

	public void _main(String[] args) throws Exception {

		CmdLineParser parser = new CmdLineParser(this);

		try {
			parser.parseArgument(args);
		} catch (CmdLineException ex) {
			System.err.println(ex.getMessage() + ".");
			parser.printUsage(System.err);
			System.exit(-1);
		}

		Logger logger = log4jInit();
		printPortInfo(logger);
		serverInit(logger);
		logger.info("Server is up. Hit CTRL+C to stop.");
		
		// Holds this thread forever, as the JMX server doesn't run on daemon
		// mode.
		while(true) {
			synchronized(this) {
				this.wait();
			}
		}
	}

	private void serverInit(Logger logger) throws Exception {		
		logger.info("Registering JMX services.");
		MBeanServer server = ManagementFactory.getPlatformMBeanServer();
		ObjectName name = new ObjectName("simulations:type=SimulationControl");

		SimulationControl control = new SimulationControl(fMasterFolder, fPort);
		control.initialize(server);
		server.registerMBean(control, name);
	}

	private void printPortInfo(Logger logger) {
		logger.info("Service RMI registry port is " + fPort + ".");
		String jmxPort = System
				.getProperty("com.sun.management.jmxremote.port");

		if (jmxPort == null) {
			logger.warn("com.sun.management.jmxremote.port undefined, "
					+ "JMX server will only be accessible locally.");
		} else {
			logger.info("JMX plaftorm server port is " + jmxPort + ".");
		}
	}

	private Logger log4jInit() {
		System.err.print("Initializing log4j -- ");

		URL url = SimControlCLI.class.getClassLoader().getResource(
				"logging.properties");
		if (url == null) {
			System.err.println("[default configuration]");
			BasicConfigurator.configure();
		} else {
			System.err.println("loaded [" + url.toString() + "]");
			PropertyConfigurator.configure(url);
		}

		return Logger.getLogger(SimControlCLI.class);
	}

	public static void main(String[] args) throws Exception {
		new SimControlCLI()._main(args);
	}
}
