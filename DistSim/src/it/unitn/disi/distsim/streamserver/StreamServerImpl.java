package it.unitn.disi.distsim.streamserver;

import it.unitn.disi.utils.streams.TimedFlusher;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.log4j.Logger;

public class StreamServerImpl implements Runnable {

	private static final int MAX_BACKLOG = 128;

	public static final int STREAM_FLUSHING_INTERVAL = 30000;

	private static final Logger fLogger = Logger
			.getLogger(StreamServerImpl.class);

	private final CopyOnWriteArrayList<ClientHandler> fActiveHandlers = new CopyOnWriteArrayList<ClientHandler>();

	private final TimedFlusher fFlusher =  new TimedFlusher(STREAM_FLUSHING_INTERVAL);

	private final File fOutput;

	private final int fPort;

	private volatile boolean fShutdownSignalled;

	private volatile Thread fShutdownHook;

	private ServerSocket fSocket;

	public StreamServerImpl(int port, File output) {
		fPort = port;
		fOutput = output;
	}

	public void run() {

		if (!fOutput.isDirectory()) {
			throw new RuntimeException(fOutput + " is not a valid folder.");
		}

		fLogger.info("Output folder is: " + fOutput + ".");

		Thread flusher = new Thread(fFlusher, "Stream Flusher");
		flusher.start();

		try {
			serverSocketCreate();
			fShutdownHook = new Thread(new Shutdown(Thread.currentThread(),
					flusher));
			Runtime.getRuntime().addShutdownHook(fShutdownHook);
			mainLoop();
		} catch (Exception ex) {
			if (!(ex instanceof SocketException) || !fShutdownSignalled) {
				fLogger.error("Server terminating with error.", ex);
			}
			serverSocketDestroy();
		}
	}

	private ServerSocket serverSocketCreate() throws Exception {
		fSocket = null;
		fSocket = new ServerSocket(fPort, MAX_BACKLOG);
		return fSocket;
	}

	private void serverSocketDestroy() {
		try {
			if (fSocket != null) {
				fSocket.close();
			}
		} catch (IOException eex) {
			fLogger.error("Error closing server socket.", eex);
		}
	}

	public int getActualPort() {
		if (fSocket != null) {
			return fSocket.getLocalPort();
		}
		return fPort;
	}

	public void stop() {
		Runtime.getRuntime().removeShutdownHook(fShutdownHook);
		fShutdownHook.run();
	}

	private void mainLoop() throws IOException {
		fLogger.info("Entering server main loop (listen port is " + fPort
				+ ").");
		Socket endpoint = null;
		try {
			while (!Thread.interrupted()) {
				endpoint = fSocket.accept();
				fLogger.info("Accepted connection from "
						+ endpoint.getRemoteSocketAddress() + ".");
				ClientHandler handler = new ClientHandler(endpoint, fOutput,
						this);

				// Thread-per-connection without pooling scales enough to our
				// use scenarios (few thousand clients which connect and then
				// maintain their connections).
				new Thread(handler).start();
				synchronized (this) {
					fActiveHandlers.add(handler);
					fFlusher.add(handler);
				}
			}
		} finally {
			try {
				if (endpoint != null) {
					endpoint.close();
				}
			} catch (IOException ex) {
				fLogger.error("Error closing socket endpoint.", ex);
			}
		}
	}

	public synchronized void handlerDone(ClientHandler clientHandler) {
		handlerError(null, clientHandler);
	}

	public synchronized void handlerError(Exception ex,
			ClientHandler clientHandler) {
		StringBuffer msg = new StringBuffer();

		msg.append("Handler for (");
		msg.append(clientHandler.clientAddress().toString());
		msg.append(", ");
		msg.append(clientHandler.clientId());
		msg.append(") terminated ");
		msg.append(ex == null ? "normally" : "with an error.");

		if (ex != null) {
			fLogger.error(msg.toString(), ex);
		} else {
			fLogger.info(msg.toString(), ex);
		}

		fActiveHandlers.remove(clientHandler);
		fFlusher.remove(clientHandler);
	}

	class Shutdown implements Runnable {

		private final Thread fMainThread;

		private final Thread fFlusherThread;

		public Shutdown(Thread main, Thread flusher) {
			fMainThread = main;
			fFlusherThread = flusher;
		}

		@Override
		public void run() {
			fLogger.info("Shutdown signalled.");
			fShutdownSignalled = true;

			fLogger.info("Stopping main loop.");

			// Interrupts main thread.
			fMainThread.interrupt();
			// Closes socket.
			serverSocketDestroy();

			try {
				fMainThread.join();
			} catch (InterruptedException e) {
				// Swallows and proceeds.
			}

			fLogger.info("Stopping buffer flusher.");
			fFlusherThread.interrupt();
			try {
				fFlusherThread.join();
			} catch (InterruptedException ex) {
				// Swallows, again...
			}

			fLogger.info("Stopping client handlers.");
			for (ClientHandler handler : fActiveHandlers) {
				try {
					handler.synchronousStop();
				} catch (InterruptedException e) {
					// Just swallows and proceeds.
				}
			}
			fLogger.info("Bye-bye.");
		}

	}

}
