package it.unitn.disi.newscasting;

import java.util.ArrayList;
import java.util.List;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.Linkable;
import peersim.core.Network;
import peersim.core.Node;
import peersim.core.Protocol;
import it.unitn.disi.ISelectionFilter;
import it.unitn.disi.newscasting.IPeerSelector;
import it.unitn.disi.test.framework.TestNetworkBuilder;

/**
 * {@link DeterministicSelector} allows a selection schedule to be assign
 * manually to each node in the network.
 * 
 * @author giuliano
 */
@AutoConfig
public class DeterministicSelector implements IPeerSelector, Protocol {

	/**
	 * Causes the selector to print each selection to the std. error output.
	 */
	private boolean fVerbose;
	
	/**
	 * Linkable from which to select nodes, or <code>null</code> if nodes are to
	 * be selected from the {@link Network} singleton.
	 */
	private int fLinkable;
	
	/**
	 * The selection schedule.
	 */
	private ArrayList<Long> fChoices = new ArrayList<Long>();
	
	/**
	 * The current selection index.
	 */
	private int fCurrent = 0;
	
	public DeterministicSelector(
			@Attribute("verbose") boolean verbose, 
			@Attribute("linkable") int linkable) {
		fVerbose = verbose;
		fLinkable = linkable;
	}

	public void addChoice(Long id) {
		fChoices.add(id);
	}

	public Node selectPeer(Node source) {
		return selectPeer(source, ISelectionFilter.ALWAYS_TRUE_FILTER);
	}

	public Node selectPeer(Node source, ISelectionFilter filter) {
		
		if (fCurrent == fChoices.size()) {
			return null;
		}
		
		Long nextId = fChoices.get(fCurrent++);
		if (nextId == null) {
			return null;
		}
		
		if (fVerbose) {
			System.err.println("[node " + source.getID() + " selects " + nextId + "]");
		}
		
		if (fLinkable == -1) {
			return networkSelect(nextId, filter);
		} else {
			return linkableSelect(source, nextId, filter);
		}
	}
	
	private Node networkSelect(long nextId, ISelectionFilter filter) {
		for(int i = 0; i < Network.size(); i++) {
			Node candidate = Network.get(i);
			if (nextId == candidate.getID()) {
				if (!filter.canSelect(candidate)) {
					return null;
				}
				
				return filter.selected(candidate);
			}
		}
		
		throw new IllegalStateException("Node " + nextId + " cannot be found.");
	}
	
	private Node linkableSelect(Node source, long nextId, ISelectionFilter filter) {
		Linkable linkable = (Linkable)source.getProtocol(fLinkable);
		int size = linkable.degree();
		for (int i = 0; i < size; i++) {
			Node candidate = linkable.getNeighbor(i);
			if (nextId == candidate.getID()) {
				if (!filter.canSelect(candidate)) {
					return null;
				}
				
				return filter.selected(candidate);
			}
		}
		
		throw new IllegalStateException("Node " + nextId + " cannot be found.");
	}

	public boolean supportsFiltering() {
		return true;
	}
	
	public Object clone() {
		try {
			DeterministicSelector selector = (DeterministicSelector) super.clone();
			selector.fChoices = new ArrayList<Long>(this.fChoices);
			return selector;
		} catch (CloneNotSupportedException ex) {
			throw new RuntimeException(ex);
		}
	}
	
	public static int assignSchedule(TestNetworkBuilder builder, int linkable, Long [][] choices) {
		List<Node> nodes = builder.getNodes();
		int pid = -1;
		for (int i = 0; i < nodes.size(); i++) {
			Node node = nodes.get(i);
			DeterministicSelector selector = new DeterministicSelector(true, linkable);
			
			for (int round = 0; round < choices[i].length; round++) {
				selector.addChoice(choices[i][round]);
			}
			pid = builder.addProtocol(node, selector);
		}
		
		return pid;
	}
}

