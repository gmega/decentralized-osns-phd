package it.unitn.disi.newscasting.experiments;

import java.util.ArrayList;
import java.util.NoSuchElementException;

import com.google.common.collect.PeekingIterator;

import it.unitn.disi.application.SimpleApplication;
import it.unitn.disi.newscasting.IApplicationInterface;
import it.unitn.disi.newscasting.IContentExchangeStrategy;
import it.unitn.disi.newscasting.internal.ICoreInterface;
import it.unitn.disi.newscasting.internal.IWritableEventStorage;
import it.unitn.disi.newscasting.internal.forwarding.HistoryForwarding;
import it.unitn.disi.util.SequentialScheduler;
import it.unitn.disi.utils.peersim.NodeRegistry;
import peersim.config.Attribute;
import peersim.config.AutoConfig;
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
	
	@Attribute
	private int repetitions;
	
	@Attribute
	private int degreeCutoff;
	
	@Attribute(defaultValue = "false")
	private boolean verbose;
	
	/**
	 * List of ID intervals for scheduling.
	 */
	private SequentialScheduler fScheduler;
	
	private Node fCurrent;
	
	private PeekingIterator<Integer> fSchedule;

	// ----------------------------------------------------------------------
	
	public DisseminationExperimentGovernor(
			@Attribute("ids") String idList) {
		fScheduler = SequentialScheduler.createScheduler(idList);
		fSchedule = fScheduler.iterator();
				
		System.err.println("-- Dissemination Governor Summary --");
		System.err.println(" * Scheduled intervals: " + idList);
	}
	
	

	@Override
	public boolean execute() {
		if (shouldScheduleNext()) {
			return scheduleNext();
		}
		return false;
	}

	/**
	 * @return whether the next node should be scheduled for transmission or
	 *         not.
	 */
	protected boolean shouldScheduleNext() {
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
	
	private boolean scheduleNext() {
		
		if (fCurrent != null && !currentApp().isSuppressingTweets()) {
			throw new IllegalStateException("Only one node should tweet at a time.");
		}
		
		// If there are no nodes left...
		if (!fSchedule.hasNext()) {
			// ... next repetition, if any left.
			repetitions--;
			if (repetitions == 0) {
				return true;
			} else {
				fSchedule = fScheduler.iterator();
			}
		}
		
		// Selects the next node, skipping neighborhoods smaller than a certain size.
		int degree = -1;
		Node nextNode = null;
		while (degree <= degreeCutoff) {
			if (verbose) {
				if (nextNode != null) {
					System.out.println("-- Skipped node " + nextNode.getID() + " (deg. " + degree + ").");
				}
			}
			nextNode = NodeRegistry.getInstance().getNode(fSchedule.next());
			Linkable sn = (Linkable) nextNode.getProtocol(linkable);
			degree = sn.degree();
		}
		
		if (nextNode == null) {
			if (verbose) {
				System.out.println("-- Reached end of schedule with null node. Restarting schedule.");
			}
			return scheduleNext();
		} else {
			setCurrentNode(nextNode);
		}
		
		currentApp().scheduleOneShot(SimpleApplication.TWEET);		
		
		return false;
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
	
	private void setNeighborhood(Node node, int state, boolean clearStorage) {
		
		Linkable lnk = (Linkable) node.getProtocol(linkable);
		int degree = lnk.degree();
		node.setFailState(state);
		
		for (int i = 0; i < degree; i++) {
			Node nei = lnk.getNeighbor(i);
			nei.setFailState(state);
			if (clearStorage) {
				ICoreInterface intf = (ICoreInterface) nei.getProtocol(sns);
				IWritableEventStorage store = (IWritableEventStorage) intf.storage();
				store.clear();
			}
		}
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
