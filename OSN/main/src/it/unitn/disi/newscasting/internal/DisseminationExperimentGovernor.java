package it.unitn.disi.newscasting.internal;

import java.util.ArrayList;
import java.util.NoSuchElementException;

import com.google.common.collect.PeekingIterator;

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
 * {@link DisseminationExperimentGovernor} will schedule one node after the other for
 * dissemination on the network.
 * 
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
	private int application;
	
	/**
	 * List of ID intervals for scheduling.
	 */
	private Scheduler fScheduler;
	
	private PeekingIterator<Integer> fSchedule;

	// ----------------------------------------------------------------------
	
	public DisseminationExperimentGovernor(
			@Attribute("ids") String idList) {
		fScheduler = createScheduler(idList);
		fSchedule = fScheduler.iterator();
	}
	
	static Scheduler createScheduler(String idList) {
		String [] intervals = idList.split(" ");
		Scheduler scheduler = new Scheduler();
		
		for (int i = 0; i < intervals.length; i++){
			String [] interval = intervals[i].split(",");
			
			if (interval.length != 2) {
				throw new RuntimeException("Malformed interval " + intervals[i] + ".");
			}
			
			scheduler.addInterval(Integer.parseInt(interval[0]), Integer.parseInt(interval[1]));
		}
		
		return scheduler;
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
		if (!fSchedule.hasNext()) {
			return false;
		}

		if (!currentApp().isSuppressingTweets()) {
			throw new IllegalStateException("Only one node should tweet at a time.");
		}
		
		currentApp().scheduleOneShot(SimpleApplication.TWEET);
		
		return false;
	}
	
	private boolean isQuiescent(Node node) {
		ICoreInterface intf = (ICoreInterface) node.getProtocol(application);
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
		INodeRegistry reg = NodeRegistry.getInstance();
		Node node = reg.getNode(fSchedule.peek().longValue());
		return node;
	}
}

class Scheduler {
	
	private final ArrayList<Integer> fIntervals = new ArrayList<Integer>();
	
	public void addInterval(int start, int end) {
		int last = Integer.MIN_VALUE;
		
		if (!fIntervals.isEmpty()) {
			last = fIntervals.get(fIntervals.size() - 1);
		}
		
		if (start < last) {
			throw new IllegalArgumentException("Intervals must be sorted.");
		}
		
		fIntervals.add(start);
		fIntervals.add(end);
		
	}
	
	public PeekingIterator<Integer> iterator() {
		
		return new PeekingIterator<Integer>() {
			
			private int fCurrent = fIntervals.get(0);

			private int fIndex;
			
			@Override
			public boolean hasNext() {
				return lower() < fIntervals.size();
			}

			@Override
			public Integer next() {
				if(!hasNext()) {
					throw new NoSuchElementException();
				}
				
				int next = fCurrent;
				
				fCurrent++;
				// If we reached the upper bound, switch to the 
				// next interval.
				if (fCurrent > fIntervals.get(upper())) {
					fIndex++;
					if (hasNext()) {
						fCurrent = fIntervals.get(lower());
					}
				}
				
				return next;
			}
			
			@Override
			public Integer peek() {
				return fCurrent;
			}
			
			private int lower() {
				return 2*fIndex;
			}
			
			private int upper() {
				return 2*fIndex + 1;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
	
}
