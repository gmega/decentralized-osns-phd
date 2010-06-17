package it.unitn.disi.application.selectors;

import it.unitn.disi.IDynamicLinkable;
import it.unitn.disi.application.interfaces.IPeerSelector;
import it.unitn.disi.application.interfaces.ISelectionFilter;
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.OrderingUtils;
import it.unitn.disi.utils.PermutingCache;
import it.unitn.disi.utils.SharedBuffer;
import it.unitn.disi.utils.SharedBuffer.BufferHandle;

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

	/** Selection will consider first the nodes that fall above this 
	 * percentile.
	 */
	private static final String PAR_PERCENTILE = "percentile";

	/**
	 * If less than psimin nodes fall into the selected percentile, use this
	 * value instead.
	 */
	private static final String PAR_PSI_MINIMUM = "psimin";
	
	// ----------------------------------------------------------------------
	// Parameter storage.
	// ----------------------------------------------------------------------

	private int fLinkable;

	private double fPsi;

	private int fPsiMinimum;

	// ----------------------------------------------------------------------
	// Protocol state.
	// ----------------------------------------------------------------------
	
	private HashMap<Node, Integer> fCentralityScores = new HashMap<Node, Integer>();
	
	private PermutingCache fFriends;
	
	// ----------------------------------------------------------------------
	// Misc.
	// ----------------------------------------------------------------------

	private Random fRandom;
	
	private final Comparator<Node> CENTRALITY_COMPARATOR = new Comparator<Node>() {
		@Override
		public int compare(Node o1, Node o2) {
			return fCentralityScores.get(o1) - fCentralityScores.get(o2);
		}
	};

	// ----------------------------------------------------------------------

	public CentralitySelector(String prefix) {
		this(Configuration.getPid(prefix + "." + PAR_PROTOCOL), Configuration
				.getDouble(prefix + "." + PAR_PERCENTILE), Configuration.getInt(prefix
				+ "." + PAR_PSI_MINIMUM), CommonState.r);
	}
	
	// ----------------------------------------------------------------------

	public CentralitySelector(int linkableId, double psi, int psiMinimum,
			Random random) {
		fLinkable = linkableId;
		fPsi = psi;
		fPsiMinimum = psiMinimum;
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
		fFriends.populate(source);
		fFriends.orderBy(CENTRALITY_COMPARATOR);
		
		// Step 3 - selects the approximate psi-th percentile.
		int start = (int) Math.floor(fPsi * (double) fFriends.size());
		// Finds whether nodes of smaller rank have equal centrality, so they can 
		// enter the sample as well.
		int cutCentrality = fCentralityScores.get(linkable.getNeighbor(start));
		do {
			start--;
		}while (fCentralityScores.get(linkable.getNeighbor(start)) == cutCentrality);
		
		start++;
		
		if ((fFriends.size() - start) < fPsiMinimum) {
			start = Math.max(0, fFriends.size() - fPsiMinimum);
		}

		// Step 4 - scrambles the head.
		OrderingUtils.permute(start, fFriends.size(), fFriends, fRandom);
		
		// Step 5 - separately scrambles the rest of the list.
		OrderingUtils.permute(0, start, fFriends, fRandom);

		// Step 6 - tries to pick someone up.
		Node selected = null;
		for (int i = fFriends.size() - 1; i >= 0; i--) {
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
		return changed;
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
			CentralitySelector adapter = (CentralitySelector) super
					.clone();
			adapter.fFriends = new PermutingCache(fLinkable);
			adapter.fCentralityScores = new HashMap<Node, Integer>(fCentralityScores);
			return adapter;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}
	
	// ----------------------------------------------------------------------
}
