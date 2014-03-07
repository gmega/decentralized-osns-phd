package it.unitn.disi.distsim.streamserver;

import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.distsim.control.SimulationControl;
import it.unitn.disi.utils.streams.FlushableGZIPOutputStream;
import it.unitn.disi.utils.streams.TimedFlusher;

import java.io.IOException;
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
	
	public static final int GZIP_FLUSH_INTERVAL = 5000;

	public static final int FILL_BACKOFF = 100;

	// Don't go for more than 2.5 secs without writing. May not be
	// so efficient for apps that pump boatloads of data at a time,
	// but provides good latency to output.
	public static final int FILL_TIMEOUT = 2500;

	@Attribute("job")
	private String fJobId;

	@Attribute("control.address")
	private String fAddress;

	@Attribute("sim-id")
	private String fSimId;

	@Attribute(value = "gzip", defaultValue = "false")
	private boolean fGZip;

	@Attribute(value = "stream.port", defaultValue = "0")
	private int fStreamPort;

	@Attribute(value = "control.port", defaultValue = "30327")
	private int fPort;
	
	private Logger fLogger;

	private Thread fFlusherThread;

	private boolean fEOF;

	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {

		configureLogging();

		OutputStream oStream = null;
		byte[] buffer = new byte[1048576];

		try {

			// Attempts to resolve port from JMX if no port is specified.
			try {
				fStreamPort = fStreamPort == 0 ? resolvePort()
						: fStreamPort;
			} catch (Exception ex) {
				fLogger.error("Can't resolve streaming port.", ex);
				return;
			}

			Socket socket = new Socket(fAddress, fStreamPort);
			oStream = socket.getOutputStream();

			sendPreamble(oStream);

			// GZip streams have to be periodically flushed or
			// results may take too long to get to the server.
			if (fGZip) {
				oStream = new FlushableGZIPOutputStream(oStream);
				startFlusher(oStream);
			}

			while (!fEOF) {
				int read = fillBuffer(is, buffer, FILL_TIMEOUT);
				if (read > 0) {
					if(fLogger.isDebugEnabled()) {
						fLogger.debug("Write " + read + " bytes.");
					}
					oStream.write(buffer, 0, read);
				}
			}

			// Socket will be closed in finally block.
			oStream.flush();

		} finally {
			// Stops the flusher if one is active.
			if (fGZip) {
				shutdownFlusher();
			}

			if (oStream != null) {
				oStream.close();
			}
		}
	}

	public void startFlusher(OutputStream oStream) {
		TimedFlusher flusher = new TimedFlusher(GZIP_FLUSH_INTERVAL);
		flusher.add(oStream);

		fFlusherThread = new Thread(flusher, "GZIP flusher thread.");
		fFlusherThread.setDaemon(true);
		fFlusherThread.start();
	}

	public void shutdownFlusher() throws InterruptedException {
		fFlusherThread.interrupt();
		fFlusherThread.join();
	}

	public int fillBuffer(InputStream is, byte[] buffer, long FILL_TIMEOUT)
			throws IOException, InterruptedException {

		long start = System.currentTimeMillis();
		boolean forceRead = false;
		int read = 0;
		
		if(fLogger.isDebugEnabled()) {
			fLogger.debug("Call to fillBuffer.");
		}

		while (true) {

			/**
			 * Ideally, we want to read until EOF is found (or we timeout).
			 * Unfortunately, available() doesn't allow us to distinguish
			 * between a read that will block and a read that will result in an
			 * EOF, as both return zero. We therefore have to bend over
			 * backwards to make it work, and that's why the 'forceRead' flag is
			 * there.
			 */
			while (is.available() > 0 || forceRead) {
				int c = is.read();
				forceRead = false;
				if (c == -1) {
					fEOF = true;
					break;
				}

				buffer[read] = (byte) c;
				read++;

				if (read == buffer.length) {
					return read;
				}
			}

			// If we found an EOF already, returns immediately.
			if (fEOF) {
				return -1;
			}

			// Backs off.
			Thread.sleep(FILL_BACKOFF);

			// Timeout.
			if ((System.currentTimeMillis() - start) > FILL_TIMEOUT) {
				System.err.println("Timed out.");
				/*
				 * If we timed out with nothing to read, forces a next iteration
				 * in which a read will occur. In that case, we'll either:
				 * 
				 * 1. block until at least 1 byte is read, which is fine as we
				 * have already returned everything there was to return on the
				 * previous call, and this data will be forwarded anyway.
				 * 
				 * 2. find an EOF and initiate termination.
				 */
				if (read == 0) {
					if(fLogger.isDebugEnabled()) {
						fLogger.debug("Next read will be forced.");
					}
					forceRead = true;
				} else {
					if(fLogger.isDebugEnabled()) {
						fLogger.debug("Read " + read + " bytes.");
					}
					break;
				}
			}
		}

		return read;
	}

	public void sendPreamble(OutputStream oStream) throws IOException {
		// Sends the id, and whether the log stream will be zipped or not.
		ObjectOutputStream stream = new ObjectOutputStream(oStream);
		stream.writeUTF(fJobId);
		stream.writeBoolean(fGZip);

		// Don't close it or we close the socket.
		stream.flush();
	}

	/**
	 * {@link #resolvePort(Logger)} allows the use of automatic port assignment
	 * on the server side for the stream socket. Actual port numbers are
	 * published on the JMX {@link StreamServerMBean}.
	 */
	private int resolvePort() throws Exception {
		// We want to access beans via plain RMI.
		JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://"
				+ fAddress + ":" + fPort + "/jmxrmi");

		fLogger.info("Contacting JMX server at " + url.toString() + ".");
		JMXConnector connector = JMXConnectorFactory.connect(url);
		MBeanServerConnection connection = connector.getMBeanServerConnection();

		// Resolves the streaming service.
		fLogger.info("Resolving streaming server mbean.");
		ObjectName streamServ = new ObjectName(SimulationControl.serviceName(
				fSimId, "outputstreamer"));
		StreamServerMBean server = (StreamServerMBean) JMX.newMBeanProxy(
				connection, streamServ, StreamServerMBean.class);

		// Retrieves port for socket server.
		fLogger.info("Querying port.");
		int port = server.getPort();

		fLogger.info("Port is " + port + ".");
		return port;
	}

	private void configureLogging() {
		BasicConfigurator.configure();
		fLogger = Logger.getLogger(OutputRedirector.class);
	}
}
