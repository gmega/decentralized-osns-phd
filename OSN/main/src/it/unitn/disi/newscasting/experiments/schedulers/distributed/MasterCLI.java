package it.unitn.disi.newscasting.experiments.schedulers.distributed;

import it.unitn.disi.newscasting.experiments.schedulers.IntervalScheduler;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

public class MasterCLI {
	
	private static final Logger fLogger = Logger.getLogger(MasterCLI.class);
	
	@Option(name = "-p", aliases = { "--port" }, usage = "Listening port to server (defaults to 50325).", required = false)
	private int fPort = 50325;
	
	@Option(name = "-i", aliases = { "--idlist" }, usage = "Semicolon-separated list of comma-separated IDs.", required = true)
	private String fIdList;
	
	public void _main(String [] args) {
		
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
		publish(new MasterImpl(scheduler));
	}
	
	private void publish(MasterImpl impl) {
		fLogger.info("Starting registry and publishing object reference.");		
		try {
			LocateRegistry.createRegistry(fPort);
			UnicastRemoteObject.exportObject(impl, 0);
			Registry registry = LocateRegistry.getRegistry(fPort);
			registry.rebind(IMaster.MASTER_ADDRESS, impl);
		} catch (RemoteException ex) {
			fLogger.error("Error while publishing object.", ex);
			System.exit(-1);
		}
		
		fLogger.info("All good.");
	}

	private void configureLogging() {
		BasicConfigurator.configure();
	}
	
	public static void main(String [] args) {
		new MasterCLI()._main(args);
	}
}
