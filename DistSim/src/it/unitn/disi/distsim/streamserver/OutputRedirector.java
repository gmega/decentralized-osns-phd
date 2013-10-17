package it.unitn.disi.distsim.streamserver;

import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.distsim.control.SimulationControl;

import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import peersim.config.Attribute;
import peersim.config.AutoConfig;

/**
 * Command line utility for shipping output streams from a process to a remote
 * server.
 * 
 * @author giuliano
 */
@AutoConfig
public class OutputRedirector implements ITransformer {

	@Attribute("job")
	private String fJobId;

	@Attribute("control.address")
	private String fAddress;

	@Attribute("sim-id")
	private String fSimId;

	@Attribute(value = "stream.port", defaultValue = "0")
	private int fStreamPort;

	@Attribute(value = "control.port", defaultValue = "30327")
	private int fPort;

	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {

		configureLogging();

		Logger logger = Logger.getLogger(OutputRedirector.class);

		Socket socket = null;
		byte[] buffer = new byte[1048576];

		try {

			// Attempts to resolve port from JMX if no port is specified.
			if (fStreamPort == 0) {
				try {
					fStreamPort = resolvePort(logger);
				} catch (Exception ex) {
					logger.error("Can't resolve streaming port.", ex);
					return;
				}
			}

			socket = new Socket(fAddress, fStreamPort);
			OutputStream oStream = socket.getOutputStream();

			// Sends the id.
			ObjectOutputStream stream = new ObjectOutputStream(oStream);
			stream.writeObject(fJobId);

			int read;
			while ((read = is.read(buffer)) != -1) {
				oStream.write(buffer, 0, read);
			}

		} finally {
			if (socket != null) {
				socket.close();
			}
		}
	}

	/**
	 * {@link #resolvePort(Logger)} allows the use of automatic port assignment
	 * on the server side for the stream socket. Actual port numbers are
	 * published on the JMX {@link StreamServerMBean}.
	 */
	private int resolvePort(Logger logger) throws Exception {
		// We want to access beans via plain RMI.
		JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://"
				+ fAddress + ":" + fPort + "/jmxrmi");

		logger.info("Contacting JMX server at " + url.toString() + ".");
		JMXConnector connector = JMXConnectorFactory.connect(url);
		MBeanServerConnection connection = connector.getMBeanServerConnection();

		// Resolves the streaming service.
		logger.info("Resolving streaming server mbean.");
		ObjectName streamServ = new ObjectName(SimulationControl.serviceName(
				fSimId, "outputstreamer"));
		StreamServerMBean server = (StreamServerMBean) JMX.newMBeanProxy(
				connection, streamServ, StreamServerMBean.class);

		// Retrieves port for socket server.
		logger.info("Querying port.");
		int port = server.getPort();

		logger.info("Port is " + port + ".");
		return port;
	}

	private void configureLogging() {
		BasicConfigurator.configure();
	}
}
