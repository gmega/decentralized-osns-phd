package it.unitn.disi.distsim.streamserver;

import java.io.File;

import it.unitn.disi.distsim.control.ISimulation;

import com.thoughtworks.xstream.annotations.XStreamAlias;

public class StreamServer implements StreamServerMBean {

	@XStreamAlias("autostart")
	private boolean fRunning;

	@XStreamAlias("port")
	private int fPort;

	private ISimulation fParent;
	
	private StreamServerImpl fServerImpl;
	
	private Thread fServerThread;

	public StreamServer(ISimulation parent) {
		fParent = parent;
		fPort = 50327;
	}

	@Override
	public synchronized void start() {
		checkNotRunning();
		fServerImpl = new StreamServerImpl(fPort, checkOutputFolder());
		fServerThread = new Thread(fServerImpl);
		fServerThread.start();
	}

	private File checkOutputFolder() {
		File output = getOutputFolder();
		if (!output.exists()) {
			output.mkdir();
		}
		
		return output;
	}

	@Override
	public synchronized void stop() {
		fServerThread.interrupt();
		try {
			fServerThread.join();
		} catch (InterruptedException e) {
			// Swallows.
		}
	}

	@Override
	public synchronized boolean isRunning() {
		return fRunning;
	}

	@Override
	public synchronized int getPort() {
		return fPort;
	}

	@Override
	public synchronized  void setPort(int port) {
		checkNotRunning();
		fPort = port;
		fParent.attributeListUpdated(this);
	}

	@Override
	public synchronized File getOutputFolder() {
		return new File(fParent.baseFolder(), "output");
	}

	@Override
	public synchronized boolean shouldAutoStart() {
		return fRunning;
	}

	@Override
	public synchronized void setSimulation(ISimulation parent) {
		fParent = parent;
	}

	private void checkNotRunning() {
		if (isRunning()) {
			throw new IllegalStateException("Server already running.");
		}
	}

}
