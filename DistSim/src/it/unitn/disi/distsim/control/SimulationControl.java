package it.unitn.disi.distsim.control;

import it.unitn.disi.distsim.dataserver.CheckpointManager;
import it.unitn.disi.distsim.scheduler.Scheduler;
import it.unitn.disi.distsim.xstream.NotificationBroadcastConverter;
import it.unitn.disi.utils.MiscUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.management.MBeanServer;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;

import org.apache.log4j.Logger;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * {@link SimulationControl} implements the scaffolding code that puts together
 * all distributed simulation services.
 * 
 * @author giuliano
 */
public class SimulationControl implements SimulationControlMBean {

	private static final Logger fLogger = Logger
			.getLogger(SimulationControl.class);

	private XStream fStream;

	private MBeanServer fServer;

	private final File fMasterFolder;

	private final File fConfig;

	private final RMIObjectManager fObjectManager;

	private final int fRegistryPort;

	private File fConfigFolder;

	private List<String> fSimulationList;

	public SimulationControl(File master, int registryPort) {
		fMasterFolder = master;
		fConfigFolder = new File(master, "config");
		fRegistryPort = registryPort;
		fObjectManager = new RMIObjectManager(fRegistryPort, false);
		fConfig = new File(fMasterFolder, "config.xml");
	}

	public synchronized void initialize(MBeanServer server) throws Exception {
		fServer = server;
		fStream = xstreamSetup();

		fObjectManager.start();

		if (!fConfigFolder.exists()) {
			fConfigFolder.mkdir();
		}

		load();

		Iterator<String> it = fSimulationList.iterator();
		while (it.hasNext()) {
			String id = it.next();
			File base = simulationFolder(id);
			if (!base.exists()) {
				fLogger.warn("Simulation " + id
						+ " no longer exists. Removing from registry.");
				it.remove();
				continue;
			}

			File config = servicesFile(id);
			if (!config.exists()) {
				fLogger.warn("Simulation " + id
						+ " has no services.xml file and will be ignored.");
				continue;
			}

			@SuppressWarnings("unchecked")
			List<Service> services = (List<Service>) fStream.fromXML(config);
			activateAndRegister(services);
		}

		save();
	}

	private XStream xstreamSetup() {
		XStream xstream = new XStream(new DomDriver());

		xstream.omitField(NotificationBroadcasterSupport.class, "notifInfo");
		xstream.omitField(NotificationBroadcasterSupport.class, "executor");
		xstream.omitField(NotificationBroadcasterSupport.class, "listenerList");

		xstream.registerConverter(new NotificationBroadcastConverter(xstream
				.getMapper(), xstream.getReflectionProvider()));

		xstream.processAnnotations(Service.class);
		xstream.processAnnotations(Scheduler.class);
		xstream.processAnnotations(CheckpointManager.class);

		return xstream;
	}

	private void activateAndRegister(List<Service> services) throws Exception {
		for (Service service : services) {
			service.bean.setControl(this);
			fServer.registerMBean(service.bean, new ObjectName(
					service.serviceName));
		}
	}

	private File servicesFile(String id) {
		return new File(simulationFolder(id), "services.xml");
	}

	private File simulationFolder(String id) {
		return new File(fMasterFolder, id);
	}

	@Override
	public synchronized void create(String id) {

		if (exists(id)) {
			throw new IllegalArgumentException("Simulation with id " + id
					+ " already exists.");
		}

		try {
			createFolder(id);

			List<Service> services = createServices(id);
			activateAndRegister(services);
			save(servicesFile(id), services);
			fSimulationList.add(id);
			save();

		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public static String name(String... nameparts) {
		StringBuffer buffer = new StringBuffer();
		for (String namepart : nameparts) {
			buffer.append(namepart);
			buffer.append("/");
		}

		buffer.deleteCharAt(buffer.length() - 1);
		return buffer.toString();
	}

	private boolean exists(String id) {
		for (String simulation : fSimulationList) {
			if (id.equals(simulation.equals(id))) {
				return true;
			}
		}

		return false;
	}

	private void save() throws IOException {
		save(fConfig, fSimulationList);
		fLogger.info("Simulation list saved.");
	}

	@SuppressWarnings("unchecked")
	private void load() throws IOException {
		if (!fConfig.exists()) {
			fSimulationList = new ArrayList<String>();
			return;
		}

		FileInputStream stream = null;
		try {
			stream = new FileInputStream(fConfig);
			fSimulationList = (List<String>) fStream.fromXML(stream);
		} finally {
			MiscUtils.safeClose(stream, true);
		}
	}

	private File createFolder(String id) throws IOException {
		File simfolder = simulationFolder(id);
		if (!simfolder.exists()) {
			simfolder.mkdir();
		}
		return simfolder;
	}

	private void save(File file, Object object) throws IOException {
		FileOutputStream stream = null;
		try {
			stream = new FileOutputStream(file);
			fStream.toXML(object, stream);
		} finally {
			MiscUtils.safeClose(stream, true);
		}

	}

	private List<Service> createServices(String id) {
		ArrayList<Service> services = new ArrayList<Service>();
		try {
			services.add(createService(id, "scheduler", new Scheduler(id, this)));
			services.add(createService(id, "checkpoint", new CheckpointManager(
					id)));
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}

		return services;
	}

	private Service createService(String simId, String serviceid,
			ManagedService master) {
		String oName = "simulations:type=" + simId + ",group=service,name="
				+ serviceid;
		return new Service(oName, master);
	}

	public RMIObjectManager objectManager() {
		return fObjectManager;
	}

	@Override
	public File getMasterOutputFolder() {
		return fMasterFolder;
	}

	@XStreamAlias("service")
	private static class Service {

		@XStreamAlias("service-id")
		public final String serviceName;

		@XStreamAlias("config")
		public final ManagedService bean;

		public Service(String name, ManagedService bean) {
			this.bean = bean;
			this.serviceName = name;
		}
	}

	@Override
	public File getConfigFolder() {
		return fConfigFolder;
	}
}
