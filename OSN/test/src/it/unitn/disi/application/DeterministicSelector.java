package it.unitn.disi.application;

import java.util.ArrayList;

import javax.management.RuntimeErrorException;

import peersim.core.Network;
import peersim.core.Node;
import peersim.core.Protocol;
import it.unitn.disi.IPeerSamplingService;
import it.unitn.disi.protocol.selectors.ISelectionFilter;

public class DeterministicSelector implements IPeerSamplingService, Protocol {
	
	private ArrayList<String> fChoices = new ArrayList<String>();
	
	private int fCurrent = 0;
	
	public DeterministicSelector(String prefix) {
		
	}
	
	public void addChoice(String id){
		fChoices.add(id);
	}

	public Node selectPeer(Node source) {
		return selectPeer(source, ISelectionFilter.ALWAYS_TRUE_FILTER);
	}

	public Node selectPeer(Node source, ISelectionFilter filter) {
		
		if (fCurrent == fChoices.size()) {
			return null;
		}
		
		String s = fChoices.get(fCurrent++);
		if (s.equals("n")) {
			return null;
		}
		
		long nextId = Long.parseLong(s);
		
		System.err.println("[node " + source.getID() + " selects " + nextId + "]");
		
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

	public boolean supportsFiltering() {
		return true;
	}
	
	public Object clone() {
		try {
			DeterministicSelector selector = (DeterministicSelector) super.clone();
			selector.fChoices = new ArrayList<String>(this.fChoices);
			return selector;
		} catch (CloneNotSupportedException ex) {
			throw new RuntimeException(ex);
		}
	}

}
