package it.unitn.disi.application;

import java.util.ArrayList;

import peersim.core.Network;
import peersim.core.Node;
import it.unitn.disi.IPeerSamplingService;
import it.unitn.disi.protocol.selectors.ISelectionFilter;

public class DeterministicSelector implements IPeerSamplingService {
	
	private ArrayList<Long> fChoices = new ArrayList<Long>();
	
	private int fCurrent = 0;
	
	public DeterministicSelector(String prefix) {
		
	}
	
	public void addChoice(Long id){
		fChoices.add(id);
	}

	public Node selectPeer(Node source) {
		return selectPeer(source, ISelectionFilter.ALWAYS_TRUE_FILTER);
	}

	public Node selectPeer(Node source, ISelectionFilter filter) {
		long nextId = fChoices.get(fCurrent++);
		
		for(int i = 0; i < Network.size(); i++) {
			Node candidate = Network.get(i);
			if (nextId == candidate.getID()) {
				if (!filter.canSelect(candidate)) {
					throw new IllegalStateException("Canoot select " + nextId
							+ " due to filtering constraints.");
				}
				
				return filter.selected(candidate);
			}
		}
		
		throw new IllegalStateException("Node " + nextId + " cannot be found.");
	}

	public boolean supportsFiltering() {
		return true;
	}

}
