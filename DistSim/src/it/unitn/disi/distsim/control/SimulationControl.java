package it.unitn.disi.distsim.control;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import it.unitn.disi.distsim.scheduler.Master;
import it.unitn.disi.utils.MiscUtils;

import javax.management.MBeanServer;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;

import org.apache.log4j.Logger;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
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

	@XStreamOmitField
	private XStream fStream;

	@XStreamOmitField
	private MBeanServer fServer;

	@XStreamOmitField
	private final File fMasterFolder;

	@XStreamOmitField
	private final File fConfig;

	@XStreamAlias("registry-port")
	private final int fRegistryPort;

	@XStreamImplicit(itemFieldName = "simulation")
	private List<String> fSimulationList = new ArrayList<String>();

	public SimulationControl(File master, int registryPort) {
		fMasterFolder = master;
		fRegistryPort = registryPort;
		fConfig = new File(fMasterFolder, "config.xml");
	}

	public synchronized void initialize(MBeanServer server) throws Exception {
		fServer = server;
		fStream = xstreamSetup();
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

			registerServices(services);
		}

		save();
	}

	private XStream xstreamSetup() {
		XStream xstream = new XStream(new DomDriver());
		
		xstream.omitField(NotificationBroadcasterSupport.class, "notifInfo");
		xstream.omitField(NotificationBroadcasterSupport.class, "executor");
		xstream.omitField(NotificationBroadcasterSupport.class, "listenerList");
		
		xstream.processAnnotations(Service.class);
		xstream.processAnnotations(Master.class);
		
		return xstream;
	}

	private void registerServices(List<Service> services) throws Exception {
		for (Service service : services) {
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
			save(servicesFile(id), services);

			fSimulationList.add(id);
			save();

		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private boolean exists(String id) {
		for(String simulation : fSimulationList) {
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
			services.add(createService(id, "scheduler", new Master(id,
					fRegistryPort)));
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}

		return services;
	}

	private Service createService(String simId, String serviceid,
			ServiceMBean master) {
		String oName = "simulations:type=" + simId + ",group=service,name="
				+ serviceid;
		return new Service(oName, master);
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
		public final ServiceMBean bean;

		public Service(String name, ServiceMBean bean) {
			this.bean = bean;
			this.serviceName = name;
		}
	}
}
