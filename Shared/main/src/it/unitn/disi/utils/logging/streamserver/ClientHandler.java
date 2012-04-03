package it.unitn.disi.utils.logging.streamserver;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketAddress;

import org.apache.log4j.Logger;

public class ClientHandler implements Runnable {

	private static final Logger fLogger = Logger.getLogger(ClientHandler.class);

	private final Socket fSocket;

	private final File fOutputFolder;

	private final StreamServer fParent;

	private volatile String fClientId;
	
	private OutputStream fFile;

	private Thread fThread;

	public ClientHandler(Socket socket, File outputFolder, StreamServer parent) {
		fSocket = socket;
		fOutputFolder = outputFolder;
		fParent = parent;
		if (!fOutputFolder.isDirectory()) {
			throw new IllegalArgumentException(
					"Output folder needs to be a folder.");
		}
	}

	@Override
	public void run() {
		try {
			run0();
			fParent.handlerDone(this);
		} catch (Exception ex) {
			fParent.handlerError(ex, this);
		} 
	}

	private void run0() throws Exception {
		InputStream is = null;

		try {
			is = fSocket.getInputStream();
			fClientId = readId(is);

			fLogger.info("Servicing client " + fClientId + " at "
					+ fSocket.getRemoteSocketAddress() + ".");

			File outFile = new File(fOutputFolder, fClientId);
			setFileStream(outFile);

			fLogger.info("Output file is " + outFile + ".");

			byte[] buffer = new byte[1048576];

			while (!Thread.interrupted()) {
				int read = is.read(buffer);
				if (read == -1) {
					break;
				}
				writeToFile(buffer, read);
			}

		} finally {
			safeClose(is);
			safeClose(fFile);
		}

	}

	private synchronized void writeToFile(byte[] buffer, int read) throws IOException {
		fFile.write(buffer, 0, read);
	}

	private synchronized void setFileStream(File outFile) throws FileNotFoundException {
		fFile = new BufferedOutputStream(new FileOutputStream(outFile));
	}
	
	public synchronized void flushToFile() throws IOException {
		if (fFile != null) {
			fFile.flush();
		}
	}

	private void safeClose(Closeable closeable) {
		try {
			if (closeable != null) {
				closeable.close();
			}
		} catch (IOException ex) {
			fLogger.error("Failed to close closeable.", ex);
		}
	}

	private String readId(InputStream is) throws IOException,
			ClassNotFoundException {
		ObjectInputStream stream = new ObjectInputStream(is);
		return (String) stream.readObject();
	}

	public String clientId() {
		return fClientId;
	}

	public SocketAddress clientAddress() {
		return fSocket.getRemoteSocketAddress();
	}

	public void synchronousStop() throws InterruptedException {
		fThread.interrupt();
		// May those who didn't make Socket a Closeable be sodomized.
		try {
			if (fSocket != null) {
				fSocket.close();
			}
		} catch (IOException ex) {
			fLogger.error("Error closing socket.", ex);
		}
		fThread.join();
	}
}
