package it.unitn.disi.distsim.control;

import it.unitn.disi.distsim.scheduler.IScheduler;
import it.unitn.disi.distsim.scheduler.IWorker;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

public class QueryTool {

	@Option(name = "-h", aliases = { "--host" }, usage = "Host for the SimulationControl RMI registry (defaults to localhost).", required = false)
	private String fHost = "localhost";

	@Option(name = "-p", aliases = { "--port" }, usage = "Listening port for the SimulationControl RMI registry (defaults to 50325).", required = false)
	private int fPort = 50325;

	@Option(name = "-r", aliases = { "--property" }, usage = "Comma-separated list of properties to query.", required = true)
	private String fProperty;

	@Argument
	private List<String> fArguments = new ArrayList<String>();

	public void _main(String[] args) throws Exception {
		CmdLineParser parser = new CmdLineParser(this);

		try {
			parser.parseArgument(args);
		} catch (CmdLineException ex) {
			System.err.println(ex.getMessage());
		}
		
		if (fArguments.size() == 0) {
			System.err.println("No simulation id specified -- nothing to do.");
			return;
		}

		String[] keys = fProperty.split(",");
		Registry registry = LocateRegistry.getRegistry(fHost, fPort);

		for (String simId : fArguments) {
			List<IWorker> workers = workers(registry, simId);
			if (workers.size() == 0) {
				System.err.println("No workers registered for " + simId + ".");
				continue;
			}
			
			int i = 1;
			for (IWorker worker : workers) {
				Properties props = worker.status();
				System.out.println("(" + simId + ", worker " + i + "): "
						+ propString(props, keys));
				i++;
			}
		}

	}

	private String propString(Properties props, String[] keys) {
		StringBuffer sbuffer = new StringBuffer();
		for (String key : keys) {
			sbuffer.append(props.get(key));
			sbuffer.append(" ");
		}
		
		return sbuffer.toString();
	}

	private List<IWorker> workers(Registry registry, String simId)
			throws Exception {
		IScheduler scheduler = (IScheduler) registry.lookup(SimulationControl
				.name(simId, "queue"));
		return scheduler.workerList();
	}

	public static void main(String[] args) throws Exception {
		new QueryTool()._main(args);
	}

}
