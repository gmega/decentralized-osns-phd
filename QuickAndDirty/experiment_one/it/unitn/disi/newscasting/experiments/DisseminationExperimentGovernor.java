package it.unitn.disi.newscasting.experiments;

import it.unitn.disi.application.ActionExecutor;
import it.unitn.disi.application.IAction;
import it.unitn.disi.application.SimpleTrafficGenerator;
import it.unitn.disi.newscasting.IApplicationInterface;
import it.unitn.disi.newscasting.IContentExchangeStrategy;
import it.unitn.disi.newscasting.internal.ICoreInterface;
import it.unitn.disi.newscasting.internal.IWritableEventStorage;
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.peersim.INodeRegistry;
import it.unitn.disi.utils.peersim.NodeRegistry;
import it.unitn.disi.utils.peersim.SNNode;

import java.util.Vector;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Linkable;
import peersim.core.Node;

import com.google.common.collect.PeekingIterator;

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
	 * {@link IApplicationInterface} protocol id.
	 */
	@Attribute("social_newscasting")
	private int sns;

	/**
	 * {@link SimpleTrafficGenerator} protocol id.
	 */
	@Attribute
	private int executor;

	/**
	 * The number of times the schedule should be repeated.
	 */
	@Attribute
	private int repetitions;

	/**
	 * The degree cutoff value. Nodes that don't meet it won't be scheduled.
	 */
	@Attribute
	private int degreeCutoff;

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

	private SNNode fCurrent;

	/**
	 * The experiment scheduler.
	 */
	private Iterable<Integer> fScheduler;
	private PeekingIterator<Integer> fSchedule;

	// ----------------------------------------------------------------------

	@SuppressWarnings("unchecked")
	public DisseminationExperimentGovernor(
			@Attribute(Attribute.PREFIX) String prefix,
			@Attribute(value = "xchg_class", defaultValue = "it.unitn.disi.newscasting.internal.forwarding.HistoryForwarding") String klass) {
		fScheduler = createScheduler(prefix);
		try {
			fClass = (Class<? extends IContentExchangeStrategy>) Class
					.forName(klass);
		} catch (Exception ex) {
			throw MiscUtils.nestRuntimeException(ex);
		}
		resetScheduler();
		publishSingleton();
	}

	// ----------------------------------------------------------------------

	@Override
	public boolean execute() {

		// Schedules the next experiment.
		if (isCurrentExperimentOver()) {
			// Runs post-unit-experiment code.
			wrapUpExperiment();
			// Schedules the next one, if any.
			if (nextUnitExperiment()) {
				System.err.println("-- Unit experiment schedule done.");
				return true;
			}
		}

		experimentCycled();

		return false;
	}

	// ----------------------------------------------------------------------

	/**
	 * Schedules the next unit experiment, if any.
	 * 
	 * @return <code>false</code> if there were still experiments to schedule,
	 *         or <code>true</code> if no experiment has been scheduled (meaning
	 *         the simulation should stop).
	 */
	private boolean nextUnitExperiment() {

		SNNode nextNode;
		while (true) {
			// Gets a suitable next node.
			nextNode = suitableNext();

			if (nextNode == null) {
				// No viable node in the current schedule.
				// If ther are still repetitions left ...
				if (--repetitions != 0) {
					// ... restarts the scheduler.
					resetScheduler();
					continue;
				} else {
					// Otherwise reports that there are no more
					// experiments to run.
					return true;
				}
			}
			break;
		}

		// Starts the next experiment.
		initializeNextExperiment(nextNode);

		// Simulation should go on.
		return false;
	}

	// ----------------------------------------------------------------------

	/**
	 * @return <code>true</code> if there is an experiment currently running, or
	 *         <code>false</code> otherwise.
	 */
	private boolean isCurrentExperimentOver() {
		Node node = currentNode();
		if (node == null) {
			return true;
		}

		Linkable sn = (Linkable) node.getProtocol(linkable);

		// Check that everyone is quiescent.
		boolean terminated = true;
		for (int i = 0; i < sn.degree(); i++) {
			Node neighbor = sn.getNeighbor(i);
			if (!isQuiescent(neighbor)) {
				terminated = false;
				break;
			}
		}

		if (!terminated || !isQuiescent(node)) {
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

	@SuppressWarnings("unchecked")
	private Iterable<Integer> createScheduler(String prefix) {
		return (Iterable<Integer>) Configuration.getInstance(prefix + "."
				+ SCHEDULER);
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
		scheduleTweet(nextNode);

		// Notifies the observers.
		for (IExperimentObserver observer : fObservers) {
			observer.experimentStart(nextNode);
		}
	}

	private void scheduleTweet(final Node nextNode) {
		ReschedulingAction action = new ReschedulingAction(ActionExecutor.TWEET);
		action.schedule(nextNode);
	}

	private void wrapUpExperiment() {
		if (fCurrent == null) {
			return;
		}

		// Notifies the observers.
		for (IExperimentObserver observer : fObservers) {
			observer.experimentEnd(fCurrent);
		}

		if (verbose && fCurrent != null) {
			System.err.println("-- Unit experiment " + fCurrent.getID()
					+ " is done.");
		}
	}

	private void experimentCycled() {
		for (IExperimentObserver observer : fObservers) {
			observer.experimentCycled(fCurrent);
		}
	}

	private void resetScheduler() {
		fSchedule = (PeekingIterator<Integer>) fScheduler.iterator();
	}

	/**
	 * Selects the next node in the current schedule, skipping neighborhoods
	 * smaller than a certain size.
	 */
	private SNNode suitableNext() {
		Node nextNode = null;
		INodeRegistry registry = NodeRegistry.getInstance();

		while (fSchedule.hasNext()) {
			Node candidate = registry.getNode(fSchedule.next());
			Linkable sn = (Linkable) candidate.getProtocol(linkable);
			if (sn.degree() >= degreeCutoff) {
				nextNode = candidate;
				break;
			}

			if (verbose) {
				if (nextNode != null) {
					System.out.println("-- Skipped node " + candidate.getID()
							+ " (deg. " + sn.degree() + ").");
				}
			}
		}

		return (SNNode) nextNode;
	}

	private void setCurrentNode(SNNode node) {
		if (fCurrent != null) {
			clearNeighborhoodState(fCurrent);
		}

		if (verbose) {
			Linkable sn = (Linkable) node.getProtocol(linkable);
			int degree = sn.degree();
			System.out.println("-- Scheduled node " + node.getID() + " (deg. "
					+ degree + ").");
		}

		fCurrent = node;
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

	private void clearStorage(Node source) {
		ICoreInterface intf = (ICoreInterface) source.getProtocol(sns);
		IWritableEventStorage store = (IWritableEventStorage) intf.storage();
		store.clear();

		// Clears all caches.
		intf.clear(source);
	}

	private boolean isQuiescent(Node node) {
		ICoreInterface intf = (ICoreInterface) node.getProtocol(sns);
		IContentExchangeStrategy strategy = (IContentExchangeStrategy) intf
				.getStrategy(fClass);
		if (strategy.status() != IContentExchangeStrategy.ActivityStatus.QUIESCENT) {
			return false;
		}

		return true;
	}

	private boolean hasSomething(Node node) {
		ICoreInterface socialNewscasting = (ICoreInterface) node
				.getProtocol(sns);
		return socialNewscasting.storage().elements() != 0;
	}

	private Node currentNode() {
		return fCurrent;
	}

	class ReschedulingAction implements IAction {

		private final IAction fDelegate;

		private int fReschedules = 0;

		public ReschedulingAction(IAction delegate) {
			fDelegate = delegate;
		}

		public void schedule(Node node) {
			ActionExecutor exec = (ActionExecutor) node.getProtocol(executor);
			exec.add(1, node, this);
		}

		@Override
		public void execute(Node node) {
			if (!node.isUp()) {
				fReschedules++;
				schedule(node);
			} else {
				if (fReschedules > 0) {
					System.err.println("RESCHEDULES: " + fReschedules);
				}

				// Make uptimes be relative to the tweet time.
				resetNeighborhoodUptimes((SNNode) node);
				fDelegate.execute(node);
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
	}
}
