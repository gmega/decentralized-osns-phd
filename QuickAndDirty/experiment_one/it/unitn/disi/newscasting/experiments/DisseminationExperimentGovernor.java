package it.unitn.disi.newscasting.experiments;

import java.util.Vector;

import com.google.common.collect.PeekingIterator;

import it.unitn.disi.application.SimpleApplication;
import it.unitn.disi.newscasting.IApplicationInterface;
import it.unitn.disi.newscasting.IContentExchangeStrategy;
import it.unitn.disi.newscasting.internal.ICoreInterface;
import it.unitn.disi.newscasting.internal.IWritableEventStorage;
import it.unitn.disi.newscasting.internal.forwarding.HistoryForwarding;
import it.unitn.disi.utils.peersim.INodeRegistry;
import it.unitn.disi.utils.peersim.NodeRegistry;
import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.GeneralNode;
import peersim.core.Linkable;
import peersim.core.Node;

/**
 * {@link DisseminationExperimentGovernor} will schedule one node after the other for
 * dissemination on the network.
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
	@Attribute
	private int sns;

	/**
	 * {@link SimpleApplication} protocol id.
	 */
	@Attribute
	private int application;
	
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
	 * Whether or not to print assorted debugging and status information.
	 */
	@Attribute(defaultValue = "false")
	private boolean verbose;
	
	// ----------------------------------------------------------------------
	// State.
	// ----------------------------------------------------------------------
	
	private Node fCurrent;
	
	/**
	 * The experiment scheduler.
	 */
	private Iterable<Integer> fScheduler;
	private PeekingIterator<Integer> fSchedule;
	
	// ----------------------------------------------------------------------
	
	public DisseminationExperimentGovernor(
			@Attribute(Attribute.PREFIX) String prefix) {
		fScheduler = createScheduler(prefix);
		resetScheduler();
		publishSingleton();
	}

	@Override
	public boolean execute() {
		// Schedules the next experiment.
		if (isCurrentExperimentOver()) {
			// Runs post-unit-experiment code. 
			wrapUpExperiment();
			// Schedules the next one, if any.
			return nextUnitExperiment();
		}
		
		experimentCycled();
		
		return false;
	}

	/**
	 * Schedules the next unit experiment, if any.
	 * 
	 * @return <code>false</code> if there were still experiments to schedule,
	 *         or <code>true</code> if no experiment has been scheduled (meaning
	 *         the simulation should stop).
	 */
	private boolean nextUnitExperiment() {
		
		Node nextNode;
		while(true) {
			// Little sanity check.
			assertTweetSuppression();
		
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

	/**
	 * @return <code>true</code> if there is an experiment currently running, or
	 *         <code>false</code> otherwise.
	 */
	private boolean isCurrentExperimentOver() {
		Node node = currentNode();
		
		if(node == null) {
			return true;
		}
		
		Linkable sn = (Linkable) node.getProtocol(linkable);
		
		for (int i = 0; i < sn.degree(); i++) {
			Node neighbor = sn.getNeighbor(i);
			if (!isQuiescent(neighbor)) {
				return false;
			}
		}
		
		return isQuiescent(node);
	}
	
	@SuppressWarnings("unchecked")
	private Iterable<Integer> createScheduler(String prefix) {
		return (Iterable<Integer>) Configuration.getInstance(prefix + "." + SCHEDULER);
	}
	
	private void publishSingleton() {
		if (fInstance != null) {
			throw new IllegalStateException(
					"Can't create more than one instance of "
							+ this.getClass().getName() + ".");
		}
		
		fInstance = this;
	}
	
	private void initializeNextExperiment(Node nextNode) {
		// Updates the current node.
		setCurrentNode(nextNode);

		// Schedules a single tweet (the unit experiment).
		currentApp().scheduleOneShot(SimpleApplication.TWEET);
		
		// Notifies the observers.
		for (IExperimentObserver observer : fObservers) {
			observer.experimentStart(nextNode);
		}
	}

	private void wrapUpExperiment() {
		if (fCurrent == null) {
			return;
		}
		
		// Notifies the observers.
		for (IExperimentObserver observer : fObservers) {
			observer.experimentEnd(fCurrent);
		}
	}
	
	private void experimentCycled() {
		for(IExperimentObserver observer : fObservers) {
			observer.experimentCycled(fCurrent);
		}
	}

	private void assertTweetSuppression() {
		if (fCurrent != null && !currentApp().isSuppressingTweets()) {
			throw new IllegalStateException("Only one node should tweet at a time.");
		}
	}

	private void resetScheduler() {
		fSchedule = (PeekingIterator<Integer>) fScheduler.iterator();
	}

	/**
	 * Selects the next node in the current schedule, skipping neighborhoods
	 * smaller than a certain size.
	 */
	private Node suitableNext() {
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
					System.out.println("-- Skipped node " + candidate.getID() + " (deg. " + sn.degree() + ").");
				}
			}
		}
		
		return nextNode;
	}
	
	private void setCurrentNode(Node node){
		if (fCurrent != null) {
			setNeighborhood(fCurrent, GeneralNode.DOWN, true);
		}
		
		if (verbose) {
			Linkable sn = (Linkable) node.getProtocol(linkable);
			int degree = sn.degree();
			System.out.println("-- Scheduled node " + node.getID() + " (deg. " + degree + ").");
		}
	
		setNeighborhood(node, GeneralNode.OK, false);
		fCurrent = node;
	}
	
	private void setNeighborhood(Node node, int state, boolean clearState) {
		
		Linkable lnk = (Linkable) node.getProtocol(linkable);
		int degree = lnk.degree();
		node.setFailState(state);
		if (clearState) {
			clearStorage(node);
		}
		
		for (int i = 0; i < degree; i++) {
			Node nei = lnk.getNeighbor(i);
			nei.setFailState(state);
			if (clearState) {
				// Clears the event storage.
				clearStorage(nei);
			}
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
		IContentExchangeStrategy strategy = (IContentExchangeStrategy) intf.getStrategy(HistoryForwarding.class);
		if (strategy.status() != IContentExchangeStrategy.ActivityStatus.QUIESCENT) {
			return false;
		}
		
		return true;
	}
	
	private SimpleApplication currentApp() {
		return (SimpleApplication) currentNode().getProtocol(application);
	}

	private Node currentNode() {
		return fCurrent;
	}
}
