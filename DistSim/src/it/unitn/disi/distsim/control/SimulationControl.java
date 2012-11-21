package it.unitn.disi.distsim.control;

import it.unitn.disi.distsim.dataserver.CheckpointManager;
import it.unitn.disi.distsim.scheduler.Scheduler;
import it.unitn.disi.distsim.streamserver.StreamServer;
import it.unitn.disi.distsim.xstream.XStreamSerializer;

import java.io.File;
import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.log4j.Logger;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * {@link SimulationControl} implements the scaffolding code that puts together
 * all distributed simulation services.
 * 
 * @author giuliano
 */
public class SimulationControl implements SimulationControlMBean {

	private static final Logger fLogger = Logger
			.getLogger(SimulationControl.class);

	private final File fMasterFolder;

	private final File fConfigFile;

	private final int fRegistryPort;

	private File fConfigFolder;

	private List<String> fSimulationKeys;

	private List<Simulation> fSimulationList;

	private final ISerializer fSerializer;

	private final RMIObjectManager fObjectManager;

	private MBeanServer fServer;

	public SimulationControl(File master, int registryPort) {
		XStreamSerializer xStream = new XStreamSerializer();
		xStream.addClass(Service.class);

		fSerializer = xStream;
		fMasterFolder = master;
		fConfigFolder = new File(master, "config");
		fRegistryPort = registryPort;
		fObjectManager = new RMIObjectManager(fRegistryPort, false);
		fConfigFile = new File(fMasterFolder, "config.xml");
		fSimulationList = new ArrayList<Simulation>();
	}

	public synchronized void initialize(MBeanServer server) throws Exception {
		fServer = server;
		fObjectManager.start();
		if (!fConfigFolder.exists()) {
			fConfigFolder.mkdir();
		}

		load();

		Iterator<String> it = fSimulationKeys.iterator();
		while (it.hasNext()) {
			String id = it.next();
			Simulation sim = Simulation.initialize(id, this);
			if (sim == null) {
				it.remove();
			}
			fSimulationList.add(sim);
		}

		save();
	}

	@Override
	public synchronized void create(String id) {
		if (exists(id)) {
			throw new IllegalArgumentException("Simulation with id " + id
					+ " already exists.");
		}

		try {
			fSimulationList.add(Simulation.initialize(id, this));
			fSimulationKeys.add(id);
			save();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public synchronized void delete(String id) {
		Simulation sim = checkedGet(id);
		sim.deactivateAndUnregister();
		File file = sim.baseFolder();
		wipeTree(file);

		fSimulationList.remove(id);
		fSimulationKeys.remove(id);

		try {
			save();
		} catch (Exception ex) {
			fLogger.error("Error saving list.", ex);
		}

	}

	@Override
	public synchronized void reset(String id) {
		Simulation sim = checkedGet(id);
		sim.resetAll();
	}

	@Override
	public File getMasterFolder() {
		return fMasterFolder;
	}

	public RMIObjectManager objectManager() {
		return fObjectManager;
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

	private void wipeTree(File root) {
		for (File child : root.listFiles()) {
			if (child.isFile()) {
				wipeTerminal(child);
			} else {
				wipeTree(child);
			}
		}
		wipeTerminal(root);
	}

	private void wipeTerminal(File root) {
		try {
			root.delete();
			fLogger.info("Deleted " + root.getName() + ".");
		} catch (Exception ex) {
			fLogger.error("Failed to delete " + root + ".", ex);
		}
	}

	private boolean exists(String id) {
		return locate(id) != null;
	}

	private Simulation locate(String id) {

		for (Simulation simulation : fSimulationList) {
			if (id.equals(simulation.id())) {
				return simulation;
			}
		}

		return null;
	}

	private void save() throws IOException {
		fSerializer.saveObject(fSimulationKeys, fConfigFile);
		fLogger.info("Simulation list saved.");
	}

	@SuppressWarnings("unchecked")
	private void load() throws IOException {
		if (!fConfigFile.exists()) {
			fSimulationKeys = new ArrayList<String>();
			return;
		}

		fSimulationKeys = (List<String>) fSerializer.loadObject(fConfigFile);
	}

	private void checkExists(String id) throws IllegalArgumentException {
		if (!exists(id)) {
			throw new IllegalArgumentException("Unknown simulation id " + id
					+ ".");
		}
	}

	private Simulation checkedGet(String id) throws IllegalArgumentException {
		checkExists(id);
		Simulation sim = locate(id);
		return sim;
	}

	public MBeanServer jmxServer() {
		return fServer;
	}

	public ISerializer serializer() {
		return fSerializer;
	}
}

@XStreamAlias("service")
class Service {

	@XStreamAlias("service-id")
	public final String serviceName;

	@XStreamAlias("config")
	public final ManagedService bean;

	public Service(String name, ManagedService bean) {
		this.bean = bean;
		this.serviceName = name;
	}
}

class Simulation implements ISimulation {

	private static final Logger fLogger = Logger.getLogger(Simulation.class);

	private final String fId;

	private File fBase;

	private List<Service> fServiceList;

	private final SimulationControl fParent;

	public static Simulation initialize(String id, SimulationControl parent)
			throws Exception {

		File base = simulationFolder(parent.getMasterFolder(), id);
		Simulation sim = new Simulation(base, id, parent);
		if (!base.exists()) {
			base.mkdir();
			return sim.create(base);
		} else {
			return sim.load(base);
		}
	}

	private static File simulationFolder(File master, String id) {
		return new File(master, id);
	}

	private Simulation(File base, String id, SimulationControl parent) {
		fId = id;
		fBase = base;
		fParent = parent;
	}

	public File baseFolder() {
		return fBase;
	}

	public void resetAll() {
		for (Service container : fServiceList) {
			ManagedService service = container.bean;
			if (service instanceof ResettableService) {
				((ResettableService) service).reset();
			}
		}
	}

	private Simulation create(File base) throws Exception {
		fServiceList = new ArrayList<Service>();
		try {
			defaultServiceList();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
		activateAndRegister();
		serviceConfigurationUpdated();
		return this;
	}

	public void defaultServiceList() {
		fServiceList.add(createService("scheduler", new Scheduler(this)));
		fServiceList.add(createService("checkpoint", new CheckpointManager(fId,
				this)));
		fServiceList
				.add(createService("outputstreamer", new StreamServer(this)));
	}

	@SuppressWarnings("unchecked")
	private Simulation load(File base) throws Exception {
		fServiceList = (List<Service>) fParent.serializer().loadObject(
				serviceFile());

		activateAndRegister();
		return this;
	}

	private Service createService(String serviceid, ManagedService master) {
		String oName = "simulations:type=" + fId + ",group=service,name="
				+ serviceid;
		return new Service(oName, master);
	}

	private File serviceFile() {
		return new File(fBase, "services.xml");
	}

	private void activateAndRegister() throws Exception {
		for (Service service : fServiceList) {
			service.bean.setSimulation(this);
			fParent.jmxServer().registerMBean(service.bean,
					new ObjectName(service.serviceName));

			if (service.bean.shouldAutoStart()) {
				fLogger.info("Autostart " + service.serviceName);
				service.bean.start();
			}
		}
	}

	public void deactivateAndUnregister() {
		for (Service descriptor : fServiceList) {
			ManagedService service = descriptor.bean;
			try {
				if (service.isRunning()) {
					service.stop();
				}
				fParent.jmxServer().unregisterMBean(
						new ObjectName(descriptor.serviceName));
			} catch (Exception ex) {
				fLogger.error("Failed to stop service "
						+ descriptor.serviceName + ".", ex);
			}

		}
	}

	public void publish(String key, Remote remote) throws RemoteException {
		fParent.objectManager().publish(remote,
				SimulationControl.name(fId, key));
	}

	public void remove(String key) {
		fParent.objectManager().remove(SimulationControl.name(fId, key));
	}

	public void attributeListUpdated(ManagedService service) {
		serviceConfigurationUpdated();
	}

	public void serviceConfigurationUpdated() {
		try {
			fParent.serializer().saveObject(fServiceList, serviceFile());
		} catch (IOException ex) {
			fLogger.error("Failed to save configuration file " + serviceFile()
					+ ".", ex);
		}
	}

	@Override
	public String id() {
		return fId;
	}

}
