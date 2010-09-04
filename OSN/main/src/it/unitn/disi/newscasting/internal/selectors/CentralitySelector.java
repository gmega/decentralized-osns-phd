package it.unitn.disi.newscasting.internal.selectors;

import it.unitn.disi.IDynamicLinkable;
import it.unitn.disi.ISelectionFilter;
import it.unitn.disi.newscasting.IPeerSelector;
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.OrderingUtils;
import it.unitn.disi.utils.peersim.PermutingCache;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Random;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Linkable;
import peersim.core.Node;
import peersim.core.Protocol;

/**
 * {@link CentralitySelector} biases selection towards nodes with higher
 * centrality.
 * 
 * @author giuliano
 */
public class CentralitySelector implements IPeerSelector, Protocol {

	// ----------------------------------------------------------------------
	// Parameter keys.
	// ----------------------------------------------------------------------
	
	/** Linkable over which selection is to take place. **/
	private static final String PAR_PROTOCOL = "linkable";

	/**
	 * Will select one amongst the psi top-ranked nodes.
	 */
	private static final String PAR_PSI = "psi";

	// ----------------------------------------------------------------------
	// Parameter storage.
	// ----------------------------------------------------------------------

	private int fLinkable;

	private int fPsi;

	// ----------------------------------------------------------------------
	// Protocol state.
	// ----------------------------------------------------------------------
	
	private HashMap<Node, Integer> fCentralityScores;
	
	private PermutingCache fFriends;
	
	// ----------------------------------------------------------------------
	// Misc.
	// ----------------------------------------------------------------------

	private Random fRandom;
	
	private Comparator<Node> fCentralityComparator;

	// ----------------------------------------------------------------------

	public CentralitySelector(String prefix) {
		this(Configuration.getPid(prefix + "." + PAR_PROTOCOL), Configuration
				.getInt(prefix + "." + PAR_PSI), CommonState.r);
	}
	
	// ----------------------------------------------------------------------

	public CentralitySelector(int linkableId, int psi, Random random) {
		fLinkable = linkableId;
		fPsi = psi;
		fRandom = random;
		
		// This is okay as PeerSim calls the constructor only once.
		fFriends = new PermutingCache(linkableId);
		fCentralityScores = new HashMap<Node, Integer>();
		fCentralityComparator = new CentralityComparator(fCentralityScores);
	}
	
	// ----------------------------------------------------------------------
	// IPeerSelector interface.
	// ----------------------------------------------------------------------

	public Node selectPeer(Node node) {
		return this.selectPeer(node, ISelectionFilter.ALWAYS_TRUE_FILTER);
	}
	
	// ----------------------------------------------------------------------

	public boolean supportsFiltering() {
		return true;
	}
	
	// ----------------------------------------------------------------------

	public Node selectPeer(Node source, ISelectionFilter filter) {
		
		// Step 1 - recompute the centrality scores if the underlying 
		// linkable has changed.
		Linkable linkable = (Linkable) source.getProtocol(fLinkable);
		if (hasChanged(linkable)) {
			this.recomputeCentralityScores(source, linkable);
		}
		
		// Step 2 - loads and sorts our neighbors by centrality.
		fFriends.populate(source, filter);
		fFriends.orderBy(fCentralityComparator);
		
		// Step 3 - Defines the "most central nodes" set. It goes from "start"
		// till the end of the list.
		int start = Math.max(0, fFriends.size() - fPsi);

		// Step 4 - scrambles the head.
		OrderingUtils.permute(start, fFriends.size(), fFriends, fRandom);
		
		// Step 5 - tries to pick someone up.
		Node selected = null;
		for (int i = fFriends.size() - 1; i >= start; i--) {
			Node candidate = fFriends.get(i);
			if (filter.canSelect(candidate)) {
				selected = candidate;
				break;
			}
		}
		
		fFriends.invalidate();
		return filter.selected(selected);
	}
	
	// ----------------------------------------------------------------------

	private boolean hasChanged(Linkable linkable) {
		boolean changed = true;
		if (linkable instanceof IDynamicLinkable) {
			changed = ((IDynamicLinkable) linkable).hasChanged(CommonState
					.getIntTime());
		}

		// Needless to say, IDynamicLinkables should be used
		// for anything but the smallest simulations.
		return changed || fCentralityScores.size() == 0;
	}
	
	// ----------------------------------------------------------------------
	
	private void recomputeCentralityScores(Node source, Linkable lnk) {
		fCentralityScores.clear();
		int degree = lnk.degree();
		for (int i = 0; i < degree; i++) {
			Node neighbor = lnk.getNeighbor(i);
			if (neighbor == null) {
				throw new IllegalStateException("Cannot deal with null neighbors.");
			}
			fCentralityScores.put(neighbor, this.centrality(source, neighbor));
		}
	}
	
	// ----------------------------------------------------------------------
	
	private int centrality(Node source, Node neighbor) {
		// Centrality is degree centrality here.
		return MiscUtils.countIntersections(source, neighbor, fLinkable);
	}
	
	// ----------------------------------------------------------------------
	// Protocol interface.
	// ----------------------------------------------------------------------

	public CentralitySelector clone() {
		try {
			CentralitySelector cloned = (CentralitySelector) super
					.clone();
			cloned.fFriends = new PermutingCache(fLinkable);
			cloned.fCentralityScores = new HashMap<Node, Integer>(fCentralityScores);
			cloned.fCentralityComparator = new CentralityComparator(cloned.fCentralityScores);
			return cloned;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}
	
	// ----------------------------------------------------------------------
	// Helper classes.
	// ----------------------------------------------------------------------
	
	private static class CentralityComparator implements Comparator<Node> {
		
		private final HashMap<Node, Integer> fCentralityScores;
		
		public CentralityComparator(HashMap<Node, Integer> centralityScores) {
			fCentralityScores = centralityScores;
		}
		
		@Override
		public int compare(Node o1, Node o2) {
			return fCentralityScores.get(o1) - fCentralityScores.get(o2);
		}
	}
	
	// ----------------------------------------------------------------------
}
