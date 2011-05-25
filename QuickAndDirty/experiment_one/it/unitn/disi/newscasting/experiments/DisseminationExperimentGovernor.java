package it.unitn.disi.newscasting.experiments;

import it.unitn.disi.epidemics.IApplicationInterface;
import it.unitn.disi.epidemics.IProtocolSet;
import it.unitn.disi.newscasting.ISocialNewscasting;
import it.unitn.disi.newscasting.IContentExchangeStrategy;
import it.unitn.disi.newscasting.experiments.schedulers.IScheduleIterator;
import it.unitn.disi.newscasting.experiments.schedulers.SchedulerFactory;
import it.unitn.disi.newscasting.internal.IWritableEventStorage;
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.logging.TabularLogManager;
import it.unitn.disi.utils.peersim.INodeRegistry;
import it.unitn.disi.utils.peersim.NodeRebootSupport;
import it.unitn.disi.utils.peersim.NodeRegistry;
import it.unitn.disi.utils.peersim.SNNode;

import java.util.Vector;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.IResolver;
import peersim.core.Control;
import peersim.core.Linkable;
import peersim.core.Node;

/**
 * {@link DisseminationExperimentGovernor} will schedule one node after the
 * other for dissemination on the network. It is the most important component in
 * the unit experiment framework.
 * 
 * NOTE: this code is quick and dirty, and as it is it's throw away code.
 * 
 * @author giuliano
 */
@AutoConfig
public class DisseminationExperimentGovernor implements Control {

	// ----------------------------------------------------------------------
	// Singleton hack for the lack of a proper configuration infrastructure
	// and component dependency management in PeerSim.
	// ----------------------------------------------------------------------

	private static DisseminationExperimentGovernor fInstance;

	private static final Vector<IExperimentObserver> fObservers = new Vector<IExperimentObserver>();

	public static DisseminationExperimentGovernor singletonInstance() {
		return fInstance;
	}

	public static void addExperimentObserver(IExperimentObserver observer) {
		fObservers.add(observer);
	}

	// ----------------------------------------------------------------------

	private static final String SCHEDULER = "scheduler";

	// ----------------------------------------------------------------------
	// Parameter storage.
	// ----------------------------------------------------------------------

	/**
	 * {@link Linkable} representing the neighborhood graph (for which
	 * quiescence is to be checked).
	 */
	@Attribute
	private int linkable;

	/**
	 * {@link ISocialNewscasting} protocol id.
	 */
	@Attribute("social_newscasting")
	private int sns;

	/**
	 * The {@link IContentExchangeStrategy} being unit experimented.
	 */
	private Class<? extends IContentExchangeStrategy> fClass;

	/**
	 * Whether or not to print assorted debugging and status information.
	 */
	@Attribute(defaultValue = "false")
	private boolean verbose;

	// ----------------------------------------------------------------------
	// State.
	// ----------------------------------------------------------------------
	
	private SchedulingState fState = SchedulingState.WAIT;
	
	private SNNode fCurrent;

	private final NodeRebootSupport fRebootSupport;
	
	private final TimeTracker fTracker;

	/**
	 * The experiment scheduler.
	 */
	private Iterable<Integer> fScheduler;
	private IScheduleIterator fSchedule;

	// ----------------------------------------------------------------------

	@SuppressWarnings("unchecked")
	public DisseminationExperimentGovernor(
			@Attribute IResolver resolver,
			@Attribute(Attribute.PREFIX) String prefix,
			@Attribute("TabularLogManager") TabularLogManager manager,
			@Attribute(value = "xchg_class", defaultValue = "it.unitn.disi.newscasting.internal.forwarding.HistoryForwarding") String klass) {
		fScheduler = createScheduler(resolver, prefix);
		try {
			fClass = (Class<? extends IContentExchangeStrategy>) Class
					.forName(klass);
		} catch (Exception ex) {
			throw MiscUtils.nestRuntimeException(ex);
		}
		fRebootSupport = new NodeRebootSupport(prefix);
		fSchedule = (IScheduleIterator) fScheduler.iterator();
		fTracker = new TimeTracker(fSchedule.remaining(), manager);
		addExperimentObserver(fTracker);
		publishSingleton();
	}

	// ----------------------------------------------------------------------

	@Override
	public boolean execute() {
		switch(fState) {
		
		case WAIT:
			System.err.println("Wait.");
			fState = scheduleNext();
			break;
			
		case RUN:
			if (isCurrentExperimentOver()) {
				// Runs post-unit-experiment code.
				wrapUpExperiment();
				// Schedules the next one, if any.
				fState = SchedulingState.WAIT;
			} 
			break;

		case DONE:
			break;
			
		}
		
		experimentCycled();
		return fState == SchedulingState.DONE;
	}
	
	// ----------------------------------------------------------------------

	public SNNode currentNode() {
		return fCurrent;
	}
	
	// ----------------------------------------------------------------------
	
	public long experimentTime() {
		return fTracker.experimentTime();
	}
	
	// ----------------------------------------------------------------------

	/**
	 * Schedules the next unit experiment.
	 */
	private SchedulingState scheduleNext() {
		if (!fSchedule.hasNext()) {
			return SchedulingState.DONE;
		}

		SNNode nextNode = toNode(fSchedule.next());
		if (nextNode == null) {
			return SchedulingState.WAIT;
		}

		// Starts the next experiment.
		initializeNextExperiment(nextNode);

		// Simulation should go on.
		return SchedulingState.RUN;
	}

	// ----------------------------------------------------------------------

	/**
	 * @return <code>true</code> if there is an experiment currently running, or
	 *         <code>false</code> otherwise.
	 */
	private boolean isCurrentExperimentOver() {
		Node node = currentNode();
		Linkable sn = (Linkable) node.getProtocol(linkable);

		// Check that everyone is quiescent.
		boolean terminated = true;
		int quiescent = 0;
		int active = 0;
		for (int i = 0; i < sn.degree(); i++) {
			Node neighbor = sn.getNeighbor(i);
			if (!isQuiescent(neighbor)) {
				terminated = false;
				active++;
//				break;
			} else {
				quiescent++;
			}
		}
		
		if (isQuiescent(node)) {
			quiescent++;
		} else {
			active++;
			terminated = false;
		}
		
		System.out.println("STT:Quiescent:" + quiescent + " Active:" + active);

		if (!terminated) {
			return false;
		}

		// If everyone is quiescent, check that at least one node had the
		// opportunity to tweet.
		terminated = false;
		for (int i = 0; i < sn.degree(); i++) {
			Node neighbor = sn.getNeighbor(i);
			if (hasSomething(neighbor)) {
				terminated = true;
				break;
			}
		}

		terminated |= hasSomething(node);

		return terminated;
	}

	private Iterable<Integer> createScheduler(IResolver resolver, String prefix) {
		return SchedulerFactory.getInstance().createScheduler(resolver,
				prefix + "." + SCHEDULER, NodeRegistry.getInstance());
	}

	private void publishSingleton() {
		if (fInstance != null) {
			throw new IllegalStateException(
					"Can't create more than one instance of "
							+ this.getClass().getName() + ".");
		}

		fInstance = this;
	}

	private void initializeNextExperiment(SNNode nextNode) {
		// Updates the current node.
		setCurrentNode(nextNode);

		// Schedules a single tweet (the unit experiment).
		tweet(nextNode);

		// Notifies the observers.
		for (IExperimentObserver observer : fObservers) {
			observer.experimentStart(nextNode);
		}
	}

	private void wrapUpExperiment() {
		// Notifies the observers.
		for (IExperimentObserver observer : fObservers) {
			observer.experimentEnd(fCurrent);
		}
		
		fTracker.printStatistics();
		
		// Clears state from the last experiment.
		clearNeighborhoodState(fCurrent);

		if (verbose && fCurrent != null) {
			System.err.println("-- Unit experiment " + fCurrent.getID()
					+ " is done.");
		}
	}

	private void experimentCycled() {
		if (fState != SchedulingState.RUN) {
			return;
		}
		
		for (IExperimentObserver observer : fObservers) {
			observer.experimentCycled(fCurrent);
		}
	}

	private SNNode toNode(Integer id) {
		if (id == null) {
			return null;
		}
		INodeRegistry registry = NodeRegistry.getInstance();
		SNNode node = (SNNode) registry.getNode(id);
		if (node == null) {
			throw new IllegalStateException("Missing node with id " + id + ".");
		}
		return node;
	}

	private void setCurrentNode(SNNode node) {
		if (!node.isUp()) {
			throw new IllegalStateException(
					"Cannot start experiment for a node that is down.");
		}

		resetNeighborhoodUptimes(node);

		fCurrent = node;

		// Initializes the nodes for the next experiment.
		runInitializers(node);

		if (verbose) {
			Linkable sn = (Linkable) node.getProtocol(linkable);
			int degree = sn.degree();
			System.out.println("-- Scheduled node " + node.getID() + " (deg. "
					+ degree + ").");
		}
	}

	private void tweet(SNNode node) {
		ISocialNewscasting intf = (ISocialNewscasting) node
				.getProtocol(sns);
		intf.postToFriends();
	}

	private void clearNeighborhoodState(SNNode node) {
		Linkable lnk = (Linkable) node.getProtocol(linkable);
		int degree = lnk.degree();
		clearStorage(node);
		for (int i = 0; i < degree; i++) {
			SNNode nei = (SNNode) lnk.getNeighbor(i);
			clearStorage(nei);
		}
	}

	private void resetNeighborhoodUptimes(SNNode node) {
		Linkable lnk = (Linkable) node.getProtocol(linkable);
		int degree = lnk.degree();
		node.clearDowntime();
		node.clearUptime();
		for (int i = 0; i < degree; i++) {
			SNNode nei = (SNNode) lnk.getNeighbor(i);
			nei.clearDowntime();
			nei.clearUptime();
		}
	}

	private void runInitializers(SNNode node) {
		Linkable lnk = (Linkable) node.getProtocol(linkable);
		int degree = lnk.degree();
		fRebootSupport.initialize(node);
		for (int i = 0; i < degree; i++) {
			SNNode nei = (SNNode) lnk.getNeighbor(i);
			fRebootSupport.initialize(nei);
		}
	}

	private void clearStorage(Node source) {
		IApplicationInterface intf = (IApplicationInterface) source.getProtocol(sns);
		IWritableEventStorage store = (IWritableEventStorage) intf.storage();
		store.clear();

		// Clears all caches.
		intf.clear(source);
	}

	private boolean isQuiescent(Node node) {
		IProtocolSet intf = (IProtocolSet) node.getProtocol(sns);
		IContentExchangeStrategy strategy = (IContentExchangeStrategy) intf
				.getStrategy(fClass);
		if (strategy.status() != IContentExchangeStrategy.ActivityStatus.QUIESCENT) {
			return false;
		}

		return true;
	}

	private boolean hasSomething(Node node) {
		IApplicationInterface socialNewscasting = (IApplicationInterface) node
				.getProtocol(sns);
		return socialNewscasting.storage().elements() != 0;
	}

	enum SchedulingState {
		RUN, WAIT, DONE
	}
}
