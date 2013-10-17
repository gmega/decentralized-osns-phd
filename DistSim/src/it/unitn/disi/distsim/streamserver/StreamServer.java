package it.unitn.disi.distsim.streamserver;

import java.io.File;

import it.unitn.disi.distsim.control.ISimulation;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

@XStreamAlias("streamserver")
public class StreamServer implements StreamServerMBean {

	@XStreamAlias("autostart")
	private boolean fRunning;

	@XStreamAlias("port")
	private int fPort;

	@XStreamOmitField
	private ISimulation fParent;

	@XStreamOmitField
	private StreamServerImpl fServerImpl;

	@XStreamOmitField
	private Thread fServerThread;

	public StreamServer(ISimulation parent) {
		fParent = parent;
		fPort = 0;
	}

	@Override
	public synchronized void start() {
		checkNotRunning();
		fServerImpl = new StreamServerImpl(fPort, checkOutputFolder());
		fServerThread = new Thread(fServerImpl, "Stream Server Loop");
		fRunning = true;
		fServerThread.start();
		fParent.attributeListUpdated(this);
	}

	private File checkOutputFolder() {
		File output = getOutputFolder();
		if (!output.exists()) {
			output.mkdir();
		}

		return output;
	}

	@Override
	public void stop() {

		if (!isRunning()) {
			return;
		}

		fServerThread.interrupt();
		fServerImpl.stop();
		try {
			fServerThread.join();
		} catch (InterruptedException e) {
			// Swallows.
		}

		synchronized (this) {
			fRunning = false;
			fParent.attributeListUpdated(this);
		}
	}

	@Override
	public synchronized boolean isRunning() {
		return fServerThread != null && fServerThread.isAlive();
	}

	@Override
	public synchronized int getPort() {
		if (!fRunning) {
			return fPort;
		}
		
		return fServerImpl.getActualPort();
	}

	@Override
	public synchronized void setPort(int port) {
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
