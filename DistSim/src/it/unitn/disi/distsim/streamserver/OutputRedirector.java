package it.unitn.disi.distsim.streamserver;

import it.unitn.disi.cli.ITransformer;

import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;

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

	@Attribute("streamserver.address")
	private String fAddress;

	@Attribute(value = "streamserver.port", defaultValue="50327")
	private int fPort;

	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {

		configureLogging();

		Logger logger = Logger.getLogger(OutputRedirector.class);

		Socket socket = null;
		byte[] buffer = new byte[1048576];
		
		try {
			
			logger.info("Contacting streaming server at " + fAddress + ":"
					+ fPort);
			socket = new Socket(fAddress, fPort);
			
			OutputStream oStream = socket.getOutputStream();
			
			// Sends the id.
			ObjectOutputStream stream = new ObjectOutputStream(oStream);
			stream.writeObject(fJobId);
			
			while (true) {
				int read = is.read(buffer);
				if (read == -1) {
					break;
				}
				oStream.write(buffer, 0, read);
			}
			
		} finally {
			if (socket != null) { 
				socket.close();
			}
		}
	}

	private void configureLogging() {
		BasicConfigurator.configure();
	}
}
