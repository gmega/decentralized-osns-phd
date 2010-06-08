package it.unitn.disi.application.selectors;

import java.util.Arrays;
import java.util.Comparator;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Node;
import it.unitn.disi.IPeerSamplingService;
import it.unitn.disi.RouletteWheel;
import it.unitn.disi.application.LinkableSortedFriendCollection;
import it.unitn.disi.protocol.selectors.ISelectionFilter;
import it.unitn.disi.utils.SharedBuffer;

/**
 * Anti-centrality privileges nodes with low centrality.  
 * 
 * @author giuliano
 */
public class AntiCentralitySelector implements IPeerSamplingService {
	
	private static final String PAR_LINKABLE = "linkable";
	

	
	private RouletteWheel fWheel;
	
	private int fLinkable;
	
	public AntiCentralitySelector(String prefix) {
		fLinkable = Configuration.getPid(prefix + "." + PAR_LINKABLE);
	}

	public Node selectPeer(Node source) {
		return selectPeer(source, ISelectionFilter.ALWAYS_TRUE_FILTER);
	}

	public Node selectPeer(Node source, ISelectionFilter filter) {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean supportsFiltering() {
		// TODO Auto-generated method stub
		return false;
	}
	
	private RouletteWheel getWheel(Node node) {
		if (fWheel != null) {
			return fWheel;
		}
		
		// Sorts neighbors by centrality (friends in common with our node).
		LinkableSortedFriendCollection col = new LinkableSortedFriendCollection(fLinkable);
		col.sortByFriendsInCommon(node);
		
		// Computes the sum of all degrees.
		double total = 0.0;
		for (int i = 0; i < col.size(); i++) {
			total += col.friendsInCommon(i) + 1;
		}
		
		// Normalizes the degree by the sum. 
		Double [] probs = new Double[col.size()];
		for (int i = 0; i < col.size(); i++) {
			probs[i] = ((double)col.friendsInCommon(i) + 1)/total;
		}
		
		// Sorts 
		Arrays.sort(probs, new Comparator<Double>() {
			public int compare(Double v1, Double v2) {
				return (int) Math.signum(v1 - v2);
			}
		});
		
		double [] probsPrim = new double[probs.length];
		for (int i = 0; i < probsPrim.length; i++) {
			probsPrim[i] = probs[i];
		}

		fWheel = new RouletteWheel(probsPrim, CommonState.r);
		return fWheel;
	}

}
