package it.unitn.disi.newscasting.experiments.schedulers.distributed;

import it.unitn.disi.newscasting.experiments.schedulers.ISchedule;
import it.unitn.disi.newscasting.experiments.schedulers.SchedulerFactory;
import it.unitn.disi.utils.HashMapResolver;
import it.unitn.disi.utils.collections.Pair;
import it.unitn.disi.utils.tabular.TableReader;
import it.unitn.disi.utils.tabular.TableWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import peersim.config.IResolver;

public class MasterCLI {

	@Option(name = "-s", aliases = { "--status" }, usage = "Master status.", required = false)
	private boolean fStatus;

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

	@Option(name = "-m", aliases = { "--mode" }, usage = "Scheduling mode.", required = false)
	private String fMode;

	@Argument
	private List<String> fProperties = new ArrayList<String>();

	public void _main(String[] args) throws IOException {

		CmdLineParser parser = new CmdLineParser(this);
		IResolver resolver = null;
		try {
			parser.parseArgument(args);
			resolver = resolver();
		} catch (CmdLineException ex) {
			System.err.println(ex.getMessage() + ".");
			parser.printUsage(System.err);
			System.exit(-1);
		}

		configureLogging();

		if (fStatus) {
			try {
				printStatus(parser);
			} catch (NotBoundException e) {
				System.err.println("Can't find queue " + fQueueId
						+ " (not bound).");				
			}
		} else {
			launch(parser, resolver);
		}
	}

	private void launch(CmdLineParser parser, IResolver resolver)
			throws IOException, FileNotFoundException {
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
				new FileWriter(fOutputLog, append)), !append, "experiment",
				"status"));
	}

	private void printStatus(CmdLineParser parser) throws RemoteException,
			NotBoundException {
		Registry registry = LocateRegistry.getRegistry(fPort);
		IMasterAdmin admin = (IMasterAdmin) registry.lookup(fQueueId);

		Pair<String, Integer>[] workers = admin.registeredWorkers();
		Arrays.sort(workers, new Comparator<Pair<String, Integer>>() {
			@Override
			public int compare(Pair<String, Integer> o1,
					Pair<String, Integer> o2) {
				if (o1.a.equals(o2.a)) {
					return o1.b.compareTo(o2.b);
				} else {
					return o1.a.compareTo(o2.a);
				}
			}
		});

		System.err.println("queue id host");

		for (Pair<String, Integer> worker : workers) {
			System.err.println(fQueueId + " " + worker.b + " " + worker.a);
		}
	}

	private IResolver resolver() throws CmdLineException {
		HashMap<String, Object> pars = new HashMap<String, Object>();

		if (fMode == null && !fStatus) {
			throw new CmdLineException("Scheduling mode required.");
		}

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
