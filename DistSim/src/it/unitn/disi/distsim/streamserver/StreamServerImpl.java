package it.unitn.disi.distsim.streamserver;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.log4j.Logger;

public class StreamServerImpl implements Runnable {

	private static final int MAX_BACKLOG = 128;

	private static final int STREAM_FLUSHING_INTERVAL = 30000;

	private static final Logger fLogger = Logger
			.getLogger(StreamServerImpl.class);

	private final CopyOnWriteArrayList<ClientHandler> fActiveHandlers = new CopyOnWriteArrayList<ClientHandler>();

	private final File fOutput;

	private final int fPort;

	private volatile boolean fShutdownSignalled;

	private volatile Shutdown fShutdownHook;

	public StreamServerImpl(int port, File output) {
		fPort = port;
		fOutput = output;
	}

	public void run() {

		if (!fOutput.isDirectory()) {
			throw new RuntimeException(fOutput + " is not a valid folder.");
		}

		fLogger.info("Output folder is: " + fOutput + ".");

		Thread flusher = new Thread(new TimedFlusher(STREAM_FLUSHING_INTERVAL));
		flusher.start();

		ServerSocket socket = null;
		try {
			socket = new ServerSocket(fPort, MAX_BACKLOG);
			fShutdownHook = new Shutdown(socket, Thread.currentThread(),
					flusher);
			Runtime.getRuntime().addShutdownHook(new Thread(fShutdownHook));
			mainLoop(socket);
		} catch (Exception ex) {
			if (!(ex instanceof SocketException) || !fShutdownSignalled) {
				fLogger.error("Server terminating with error.", ex);
			}
			try {
				socket.close();
			} catch (IOException eex) {
				fLogger.error("Error closing server socket.", eex);
			}
		}
	}

	private void mainLoop(ServerSocket socket) throws IOException {
		fLogger.info("Entering server main loop (listen port is " + fPort
				+ ").");
		Socket endpoint = null;
		try {
			while (!Thread.interrupted()) {
				endpoint = socket.accept();
				fLogger.info("Accepted connection from "
						+ endpoint.getRemoteSocketAddress() + ".");
				ClientHandler handler = new ClientHandler(endpoint, fOutput,
						this);
				new Thread(handler).start();
				synchronized (this) {
					fActiveHandlers.add(handler);
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
	}

	class Shutdown implements Runnable {

		private final ServerSocket fServerSocket;

		private final Thread fMainThread;

		private final Thread fFlusherThread;

		public Shutdown(ServerSocket serverSocket, Thread main, Thread flusher) {
			fServerSocket = serverSocket;
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
			closeSocket();

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

		private void closeSocket() {
			try {
				fServerSocket.close();
			} catch (Exception ex) {
				fLogger.error("Error closing server socket.", ex);
			}
		}
	}

	class TimedFlusher implements Runnable {

		private final long fFlushInterval;

		public TimedFlusher(long flushInterval) {
			fFlushInterval = flushInterval;
		}

		@Override
		public void run() {
			while (!Thread.interrupted()) {
				for (ClientHandler handler : fActiveHandlers) {
					try {
						handler.flushToFile();
					} catch (IOException ex) {
						fLogger.error("Failed to flush buffer", ex);
					}
				}
				try {
					Thread.sleep(fFlushInterval);
				} catch (InterruptedException e) {
					// Restore interruption state.
					Thread.currentThread().interrupt();
				}
			}
		}
	}

}
