package it.unitn.disi.distsim.control;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.management.Attribute;
import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

/**
 * Very simple command line client to {@link SimulationControl} that serves as a
 * scriptable alternative to JConsole. Allows the execution of simple JMX
 * operations for creating, deleting, resetting and configuring simulations.
 * 
 * @author giuliano
 */
public class CommandLineClient {

	static enum Operation {
		list, create, delete, reset, set, invoke;
	}

	@Option(name = "-h", aliases = { "--host" }, usage = "Controller host.", required = true)
	private String fHost;

	@Option(name = "-p", aliases = { "--port" }, usage = "Controller port (defaults to 30327).", required = false)
	private int fPort = 30327;

	@Option(name = "-o", aliases = { "--operation" }, usage = "Creates a simulation.", required = true)
	private Operation fOperation;

	@Argument
	private List<String> fArguments = new ArrayList<String>();

	private MBeanServerConnection fConnection;

	private SimulationControlMBean fControl;

	public void _main(String[] args) throws Exception {
		CmdLineParser parser = new CmdLineParser(this);

		try {
			parser.parseArgument(args);
		} catch (CmdLineException ex) {
			System.err.println(ex.getMessage() + ".");
			parser.printUsage(System.err);
			System.exit(-1);
		}

		switch (fOperation) {

		case list: {
			list();
			break;
		}

		case create: {
			String simId = getSimId();
			if (simId == null) {
				break;
			}

			create(simId);
			break;
		}

		case delete: {
			String simId = getSimId();
			if (simId == null) {
				break;
			}

			delete(simId);
			break;
		}

		case reset: {
			String simId = getSimId();
			if (simId == null) {
				break;
			}

			reset(simId);
			break;
		}

		case set: {
			ParsedParameters pars = getParameters(4,
					"Required: simulation id, service, property, and value.");
			if (pars == null) {
				break;
			}
			setAttribute(pars);
			break;
		}

		case invoke: {
			ParsedParameters pars = getParameters(3,
					"Required: simulation id, service, property, and value.");
			if (pars == null) {
				break;
			}
			invoke(pars);
			break;
		}

		}
	}

	private ParsedParameters getParameters(int i, String string) {
		if (fArguments.size() < i) {
			error(string);
			return null;
		}

		return new ParsedParameters(fArguments);
	}

	private String getSimId() {
		if (fArguments.size() == 0) {
			error("Missing simulation id.");
			return null;
		}
		return fArguments.get(0);
	}

	private MBeanServerConnection connection() throws Exception {
		if (fConnection == null) {
			JMXServiceURL url = new JMXServiceURL(
					"service:jmx:rmi:///jndi/rmi://" + fHost + ":" + fPort
							+ "/jmxrmi");
			JMXConnector connector = JMXConnectorFactory.connect(url, null);

			fConnection = connector.getMBeanServerConnection();
		}
		return fConnection;
	}

	private SimulationControlMBean control() throws Exception {
		if (fControl == null) {
			fControl = JMX.newMBeanProxy(connection(), new ObjectName(
					"simulations:type=SimulationControl"),
					SimulationControlMBean.class);
		}

		return fControl;
	}

	public void list() throws Exception {
		List<String> ids = control().list();
		for (String id : ids) {
			out(id);
		}
	}

	public void create(String id) throws Exception {
		try {
			control().create(id);
		} catch (IllegalArgumentException ex) {
			error(ex.getMessage());
		}
	}

	public void delete(String id) throws Exception {
		control().delete(id);
	}

	public void reset(String id) throws Exception {
		control().reset(id);
	}

	public void setAttribute(ParsedParameters pars) throws Exception {
		connection().setAttribute(
				new ObjectName(SimulationControl.serviceName(pars.simId,
						pars.service)),
				new Attribute(pars.symbol, pars.parameters.get(0)));
	}

	public void invoke(ParsedParameters pars) throws Exception {
		Object result = connection().invoke(
				new ObjectName(SimulationControl.serviceName(pars.simId,
						pars.service)), pars.symbol, null, null);

		if (result != null) {
			out("Returned: " + result.toString());
		}

	}

	private void error(String message) {
		System.err.println(message);
	}

	private void out(String message) {
		System.out.println(message);
	}

	static class ParsedParameters {

		public final String simId;

		public final String service;

		public final String symbol;

		public final List<String> parameters;

		public ParsedParameters(List<String> pars) {
			simId = pars.get(0);
			service = pars.get(1);
			symbol = pars.get(2);
			parameters = Collections.unmodifiableList(pars.subList(3,
					pars.size()));
		}
	}

	public static void main(String[] args) throws Exception {
		new CommandLineClient()._main(args);
	}

}
