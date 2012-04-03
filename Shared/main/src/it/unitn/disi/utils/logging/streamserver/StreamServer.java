package it.unitn.disi.utils.logging.streamserver;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

public class StreamServer {

	private static final int MAX_BACKLOG = 128;

	@Option(name = "-p", aliases = { "--port" }, usage = "Listening port to server (defaults to 50327).", required = false)
	private int fPort = 50327;

	@Option(name = "-f", aliases = { "--folder" }, usage = "Folder where to store outputs.", required = true)
	private File fOutput;

	private final ArrayList<ClientHandler> fActiveHandlers = new ArrayList<ClientHandler>();

	private final Logger fLogger;

	private final Thread fThread;

	private volatile boolean fShutdownSignalled;

	private StreamServer(Thread thread) {
		configureLogging();
		fLogger = Logger.getLogger(StreamServer.class);
		fThread = thread;
	}

	public void _main(String[] args) {
		CmdLineParser parser = new CmdLineParser(this);
		try {
			parser.parseArgument(args);
		} catch (CmdLineException ex) {
			System.err.println(ex.getMessage() + ".");
			parser.printUsage(System.err);
			System.exit(-1);
		}

		if (!fOutput.isDirectory()) {
			System.err.println(fOutput + " is not a valid folder.");
			System.exit(-1);
		}

		fLogger.info("Output folder is: " + fOutput + ".");

		ServerSocket socket = null;
		try {
			socket = new ServerSocket(fPort, MAX_BACKLOG);
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
		Runtime.getRuntime().addShutdownHook(new Thread(new Shutdown(socket)));
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

		fLogger.error(msg.toString(), ex);

		fActiveHandlers.remove(clientHandler);
	}

	private void configureLogging() {
		BasicConfigurator.configure();
	}

	class Shutdown implements Runnable {

		private final ServerSocket fServerSocket;

		public Shutdown(ServerSocket serverSocket) {
			fServerSocket = serverSocket;
		}

		@Override
		public void run() {
			fLogger.info("Shutdown signalled.");
			fShutdownSignalled = true;
			
			fLogger.info("Stopping main loop.");

			// Interrupts main thread.
			fThread.interrupt();
			// Closes socket.
			closeSocket();

			try {
				fThread.join();
			} catch (InterruptedException e) {
				// Swallows and proceeds.
			}

			fLogger.info("Stopping client handlers.");
			ClientHandler[] copy;
			synchronized (this) {
				copy = fActiveHandlers
						.toArray(new ClientHandler[fActiveHandlers.size()]);
			}

			for (ClientHandler handler : copy) {
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

	public static void main(String[] args) throws IOException {
		new StreamServer(Thread.currentThread())._main(args);
	}
}
