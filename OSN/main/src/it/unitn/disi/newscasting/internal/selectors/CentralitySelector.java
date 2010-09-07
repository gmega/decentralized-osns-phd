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
	// Shared state.
	// ----------------------------------------------------------------------
	private static final CentralityComparator fCentralityComparator = new CentralityComparator();

	// ----------------------------------------------------------------------
	// Parameter storage.
	// ----------------------------------------------------------------------

	private int fLinkable;

	private double fPsi;

	// ----------------------------------------------------------------------
	// Protocol state.
	// ----------------------------------------------------------------------
	
	private HashMap<Node, Integer> fCentralityScores;
	
	private PermutingCache fFriends;
	
	// ----------------------------------------------------------------------
	// Misc.
	// ----------------------------------------------------------------------

	private Random fRandom;
	
	// ----------------------------------------------------------------------

	public CentralitySelector(String prefix) {
		this(Configuration.getPid(prefix + "." + PAR_PROTOCOL), Configuration
				.getDouble(prefix + "." + PAR_PSI), CommonState.r);
	}
	
	// ----------------------------------------------------------------------

	public CentralitySelector(int linkableId, double psi, Random random) {
		fLinkable = linkableId;
		fPsi = psi;
		fRandom = random;
		
		// This is okay as PeerSim calls the constructor only once.
		fFriends = new PermutingCache(linkableId);
		fCentralityScores = new HashMap<Node, Integer>();
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
		fCentralityComparator.set(fCentralityScores);
		fFriends.populate(source, filter);
		fFriends.orderBy(fCentralityComparator);
		
		// Step 3 - Defines where the "most central nodes" are.
		int start = cut();

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
		fCentralityComparator.set(null);
		return filter.selected(selected);
	}
	
	// ----------------------------------------------------------------------
	
	private int cut() {
		if (fPsi >= 1.0) {
			return Math.max(0, fFriends.size() - (int) fPsi);	
		} else {
			int cut = (int) Math.floor(fFriends.size() * fPsi);
			// Now checks to see if the nodes to the left are the same
			// centrality.
			for (; cut > 0; cut--) {
				if (fCentralityScores.get(fFriends.get(cut)) != fCentralityScores
						.get(fFriends.get(cut - 1))) {
					break;
				}
			}
			return cut;
		}
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
	// ICachingObject interface.
	// ----------------------------------------------------------------------
	@Override
	public void clear(Node source) {
		fCentralityScores.clear();
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
			return cloned;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}
	
	// ----------------------------------------------------------------------
	// Helper classes.
	// ----------------------------------------------------------------------
	
	private static class CentralityComparator implements Comparator<Node> {
		
		private HashMap<Node, Integer> fCentralityScores;
		
		public void set(HashMap<Node, Integer> centralityScores) {
			fCentralityScores = centralityScores;
		}
		
		@Override
		public int compare(Node o1, Node o2) {
			return fCentralityScores.get(o1) - fCentralityScores.get(o2);
		}
	}
	
	// ----------------------------------------------------------------------
}
