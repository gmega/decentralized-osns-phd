package it.unitn.disi.newscasting.internal.selectors;

import it.unitn.disi.ISelectionFilter;
import it.unitn.disi.newscasting.IPeerSelector;
import it.unitn.disi.utils.IReference;
import it.unitn.disi.utils.peersim.IDynamicLinkable;
import it.unitn.disi.utils.peersim.PermutingCache;
import it.unitn.disi.utils.peersim.ProtocolReference;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Random;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.CommonState;
import peersim.core.Linkable;
import peersim.core.Node;
import peersim.core.Protocol;

/**
 * {@link PercentileCentralitySelector} biases selection towards nodes with higher
 * centrality.
 * 
 * @author giuliano
 */
@AutoConfig
public class PercentileCentralitySelector implements IPeerSelector, Protocol {

	// ----------------------------------------------------------------------
	// Shared state.
	// ----------------------------------------------------------------------
	private static final CentralityComparator fCentralityComparator = new CentralityComparator();

	// ----------------------------------------------------------------------
	// Parameter storage.
	// ----------------------------------------------------------------------

	private IReference<Linkable> fLinkable;

	private IReference<IUtilityFunction> fUtility;

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

	public PercentileCentralitySelector(
			@Attribute("linkable") int linkableId,
			@Attribute("ranking") int ranking,
			@Attribute("psi") double psi) {
		this(new ProtocolReference<Linkable>(linkableId),
				new ProtocolReference<IUtilityFunction>(ranking), psi,
				CommonState.r);
	}

	// ----------------------------------------------------------------------

	public PercentileCentralitySelector(IReference<Linkable> linkableId,
			IReference<IUtilityFunction> ranking, double psi,
			Random random) {
		fLinkable = linkableId;
		fPsi = psi;
		fRandom = random;
		fUtility = ranking;

		// This is okay as PeerSim calls the constructor only once.
		fFriends = new PermutingCache(linkableId);
		fCentralityScores = new HashMap<Node, Integer>();
	}

	// ----------------------------------------------------------------------

	public void setPSI(double psi) {
		this.fPsi = psi;
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
		Linkable linkable = fLinkable.get(source);
		if (hasChanged(fUtility.get(source), linkable)) {
			this.recomputeCentralityScores(source, linkable);
		}

		// Step 2 - loads and sorts our neighbors by centrality.
		fFriends.populate(source, filter);
		if (fFriends.size() == 0) {
			// Nothing to choose from.
			return null;
		}
		fCentralityComparator.set(fCentralityScores);
		fFriends.orderBy(fCentralityComparator);
		
		// Step 3 - Defines where the "most central nodes" are.
		int start = cut();

		// Step 4 - picks a random node from the head.
		Node selected = fFriends.get(start + fRandom.nextInt(fFriends.size() - start));

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

	private boolean hasChanged(IUtilityFunction utility, Linkable linkable) {
		boolean changed = true;
		if (linkable instanceof IDynamicLinkable) {
			changed = ((IDynamicLinkable) linkable).hasChanged(CommonState
					.getIntTime());
		}
		
		// Needless to say, IDynamicLinkables should be used
		// for anything but the smallest simulations.
		return utility.isDynamic() || changed || fCentralityScores.size() == 0;
	}

	// ----------------------------------------------------------------------

	private void recomputeCentralityScores(Node source, Linkable lnk) {
		fCentralityScores.clear();
		IUtilityFunction f = (IUtilityFunction) fUtility.get(source);
		
		int degree = lnk.degree();
		for (int i = 0; i < degree; i++) {
			Node neighbor = lnk.getNeighbor(i);
			if (neighbor == null) {
				throw new IllegalStateException(
						"Cannot deal with null neighbors.");
			}
			fCentralityScores.put(neighbor, f.utility(source, neighbor));
		}
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

	public PercentileCentralitySelector clone() {
		try {
			PercentileCentralitySelector cloned = (PercentileCentralitySelector) super.clone();
			cloned.fFriends = new PermutingCache(fLinkable);
			cloned.fCentralityScores = new HashMap<Node, Integer>(
					fCentralityScores);
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
