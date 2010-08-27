package it.unitn.disi.newscasting.internal;

import it.unitn.disi.application.SimpleApplication;
import it.unitn.disi.newscasting.IApplicationInterface;
import it.unitn.disi.newscasting.IContentExchangeStrategy;
import it.unitn.disi.newscasting.internal.forwarding.HistoryForwarding;
import it.unitn.disi.utils.peersim.INodeRegistry;
import it.unitn.disi.utils.peersim.NodeRegistry;
import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.Control;
import peersim.core.Linkable;
import peersim.core.Node;

/**
 * {@link SingletonDissemination} will schedule one node after the other for
 * dissemination on the network.
 * 
 * 
 * @author giuliano
 */
@AutoConfig
public class SingletonDissemination implements Control {

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
	private int application;
	
	/**
	 * List of ID intervals for scheduling.
	 */
	private final long [] fIdList;
	
	// ----------------------------------------------------------------------
	// State.
	// ----------------------------------------------------------------------

	private int fIndex;
	
	private Long fCurrent;
	
	// ----------------------------------------------------------------------
	
	public SingletonDissemination(
			@Attribute("ids") String idList) {
		
		String [] intervals = idList.split(" ");
		fIdList = new long[intervals.length * 2];
		for (int i = 0; i < fIdList.length; i++){
			String [] interval = intervals[i].split(",");
			
			if (interval.length != 2) {
				throw new RuntimeException("Malformed interval " + intervals[i] + ".");
			}
			
			fIdList[2*i] = Long.parseLong(interval[0]);
			fIdList[2*i + 1] = Long.parseLong(interval[1]);
		}
		
		fCurrent = fIdList[0];
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
		fCurrent++;
		
		// Did we just fall off the current interval? 
		if (fCurrent > fIdList[2*fIndex + 1]) {
			fIndex++;
			// No more intervals to schedule: simulation is over.
			if (fIndex >= fIdList.length) {
				return true;
			}
			
			fCurrent = fIdList[2*fIndex];
		}
		
		if (!currentApp().isSuppressingTweets()) {
			throw new IllegalStateException("Only one node should tweet at a time.");
		}
		
		currentApp().scheduleOneShot(SimpleApplication.TWEET);
		
		return false;
	}
	
	private boolean isQuiescent(Node node) {
		ICoreInterface intf = (ICoreInterface) node.getProtocol(application);
		IContentExchangeStrategy strategy = (IContentExchangeStrategy) intf.getAdapter(HistoryForwarding.class, null);
		if (strategy.status() != IContentExchangeStrategy.ActivityStatus.QUIESCENT) {
			return false;
		}
		
		return true;
	}
	
	private SimpleApplication currentApp() {
		return (SimpleApplication) currentNode().getProtocol(application);
	}

	private Node currentNode() {
		INodeRegistry reg = NodeRegistry.getInstance();
		Node node = reg.getNode(fCurrent.longValue());
		return node;
	}
}
