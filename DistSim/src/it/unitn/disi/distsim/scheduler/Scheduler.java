package it.unitn.disi.distsim.scheduler;

import it.unitn.disi.distsim.control.ISimulation;
import it.unitn.disi.distsim.control.ManagedService;
import it.unitn.disi.distsim.scheduler.generators.ISchedule;
import it.unitn.disi.distsim.scheduler.generators.Schedulers;
import it.unitn.disi.distsim.scheduler.generators.Schedulers.SchedulerType;
import it.unitn.disi.utils.HashMapResolver;
import it.unitn.disi.utils.tabular.TableReader;
import it.unitn.disi.utils.tabular.TableWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.rmi.RemoteException;
import java.util.HashMap;

import javax.management.AttributeChangeNotification;
import javax.management.NotificationBroadcasterSupport;

import org.apache.log4j.Logger;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

@XStreamAlias("scheduler")
public class Scheduler extends NotificationBroadcasterSupport implements
		SchedulerMBean, ManagedService {

	@XStreamOmitField
	private ISimulation fControl;

	@XStreamOmitField
	private int fSequence;

	@XStreamAlias("scheduler-type")
	private SchedulerType fSchedulerType;

	@XStreamAlias("scheduler-config")
	private HashMap<String, String> fProperties = new HashMap<String, String>();

	@XStreamAlias("autostart")
	private boolean fRunning;

	@XStreamOmitField
	private SchedulerImpl fMaster = null;

	public Scheduler(ISimulation parent) {
		fControl = parent;
	}

	@SuppressWarnings("unused")
	private Scheduler() {
		super();
	}

	@Override
	public synchronized void start() {
		checkNotRunning();

		try {
			TableReader replay = replayLog();
			SchedulerImpl impl = new SchedulerImpl(createSchedule(),
					writeLog(), Logger.getLogger(loggerName("assignment")),
					Logger.getLogger(loggerName("messages")));

			if (replay != null) {
				impl.replayLog(replay);
			}

			fMaster = impl;
			fControl.publish("queue", impl);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}

		fMaster.start();

		sendNotification(new AttributeChangeNotification(this, fSequence++,
				System.currentTimeMillis(), "Scheduler [" + fControl.id()
						+ "] started.", "isRunning", "boolean", false, true));

		fRunning = true;
		fControl.attributeListUpdated(this);
	}
	
	@Override
	public void reset() {
		boolean wasRunning = fRunning;
		// Stops the scheduler.
		if (fRunning) {
			stop();
		}

		// Blows up the log.
		File replayLog = getReplayLog();
		if (replayLog.exists()) {
			replayLog.delete();
		}
		
		// Restarts.
		if (wasRunning) {
			start();
		}
	}

	private TableReader replayLog() throws IOException {
		File replayLog = getReplayLog();
		if (replayLog.exists()) {
			return new TableReader(new FileInputStream(replayLog));
		}
		return null;
	}

	private TableWriter writeLog() throws IOException {
		File replayLog = getReplayLog();
		boolean append = replayLog.exists();
		PrintWriter out = new PrintWriter(new FileOutputStream(replayLog,
				append));
		TableWriter writer = new TableWriter(out, append, "experiment",
				"status");

		// Eagerly prints header so we have no problems when reading back file.
		writer.emmitHeader();
		out.flush();

		return writer;
	}

	private ISchedule createSchedule() {
		return Schedulers.get(fSchedulerType, new HashMapResolver(fProperties));
	}

	private void checkNotRunning() throws IllegalStateException {
		if (fMaster != null) {
			throw new IllegalStateException("Server already running.");
		}
	}

	@Override
	public synchronized void stop() {
		fMaster.shutdown();
		fMaster = null;
		sendNotification(new AttributeChangeNotification(this, fSequence++,
				System.currentTimeMillis(), "Scheduler [" + fControl.id()
						+ "] stopped.", "isRunning", "boolean", false, true));
		fRunning = false;
		fControl.attributeListUpdated(this);
	}

	@Override
	public boolean isRunning() {
		return fMaster != null;
	}

	@Override
	public void setSchedulerType(String type) {
		checkNotRunning();
		fSchedulerType = SchedulerType.valueOf(type.toUpperCase());
		fControl.attributeListUpdated(this);
	}

	@Override
	public String getSchedulerType() {
		return fSchedulerType == null ? "none" : fSchedulerType.toString();
	}

	@Override
	public void setSchedulerProperties(String properties) {
		checkNotRunning();
		HashMap<String, String> props = new HashMap<String, String>();

		String[] items = properties.split(":");
		for (String item : items) {
			String[] pair = item.split("=");
			if (pair.length != 2) {
				throw new IllegalArgumentException("Malformed property ["
						+ item + "].");
			}
			props.put(pair[0], pair[1]);
		}

		fProperties = props;
		fControl.attributeListUpdated(this);
	}

	@Override
	public String getSchedulerProperties() {
		return fProperties.toString();
	}

	private String loggerName(String string) {
		return Scheduler.class.getName() + "." + fControl.id() + "." + string;
	}

	@Override
	public File getReplayLog() {
		return new File(fControl.baseFolder(), fControl.id() + "-scheduler.log");
	}

	@Override
	public void setSimulation(ISimulation parent) {
		fControl = parent;
	}

	@Override
	public boolean shouldAutoStart() {
		return fRunning;
	}

	@Override
	public int getTotal() {
		if (fMaster == null) {
			return 0;
		}
		
		return fMaster.total();
	}

	@Override
	public int getRemaining() {
		if (fMaster == null) {
			return 0;
		}
		
		try {
			return fMaster.remaining();
		} catch (RemoteException e) {
			// Shouldn't happen.
			throw new RuntimeException(e);
		}
	}
	
}
