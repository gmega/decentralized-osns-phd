package it.unitn.disi.sps;

import it.unitn.disi.ISelectionFilter;
import it.unitn.disi.newscasting.IPeerSelector;
import it.unitn.disi.newscasting.internal.selectors.CentralitySelector;
import it.unitn.disi.newscasting.internal.selectors.IUtilityFunction;
import it.unitn.disi.sps.cyclon.CyclonSN;
import it.unitn.disi.sps.selectors.TabooSelectionFilter;
import it.unitn.disi.utils.collections.StaticVector;
import it.unitn.disi.utils.peersim.FallThroughReference;
import it.unitn.disi.utils.peersim.PeersimUtils;
import it.unitn.disi.utils.peersim.PermutingCache;
import it.unitn.disi.utils.peersim.ProtocolReference;

import java.util.BitSet;

import peersim.cdsim.CDProtocol;
import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.CommonState;
import peersim.core.Linkable;
import peersim.core.Node;

/**
 * Protocol which tries to reconstruct the F2F graph.
 * 
 * @author giuliano
 */
@AutoConfig
public class F2FOverlayCollector implements CDProtocol {

	private static final StaticVector<Integer> fIndexes = new StaticVector<Integer>();

	enum QueryNeighborhood {
		NOPROACTIVE, PEERSAMPLING, COLLECTOR;
	}

	enum SelectionMode {
		FIRSTFIT, SHUFFLING, HIGHESTRANKING;
	}

	@Attribute("social_neighbourhood")
	private int fStatic;

	@Attribute("sps")
	private int fSampled;

	private IPeerSelector fSelector;

	private QueryNeighborhood fMode;

	private SelectionMode fSelectionMode;

	private BitSet fSeen;
	
	private TabooSelectionFilter fTabooFilter;

	private int fCasualHits;

	private int fProactiveHits;

	public F2FOverlayCollector(
			@Attribute("query_neighborhood") String query,
			@Attribute("selection_mode") String mode,
			@Attribute(value = "taboo", defaultValue = "0") int tabooSize) {
		fMode = QueryNeighborhood.valueOf(query.toUpperCase());
		fSelectionMode = SelectionMode.valueOf(mode.toUpperCase());
		if (tabooSize > 0) {
			fTabooFilter = new TabooSelectionFilter(tabooSize);
		}
		fSelector = configureSelector();
	}

	private IPeerSelector configureSelector() {
		IPeerSelector selector = null;
		switch (fSelectionMode) {
		case HIGHESTRANKING:
			selector = new CentralitySelector(new ProtocolReference<Linkable>(
					fSampled), new FallThroughReference<IUtilityFunction>(
					new IntersectingUnseenUtility()), 1.0, CommonState.r);
			break;
		case FIRSTFIT:
			selector = new FirstFitSelector(false);
			break;
		case SHUFFLING:
			selector = new FirstFitSelector(true);
			break;
		}
		return selector;
	}

	@Override
	public void nextCycle(Node node, int protocolID) {
		Linkable statik = (Linkable) node.getProtocol(fStatic);
		CyclonSN sampled = (CyclonSN) node.getProtocol(fSampled);
		
		fIndexes.clear();
		
		init(statik);
		resetCounters();

		// Collect friends from the peer sampling layer (if any).
		collectFriends(statik, sampled);

		// Proactively tries to obtain new entries.
		if (fMode != QueryNeighborhood.NOPROACTIVE) {
			proactiveQuery(node);
		}
	}

	private void collectFriends(Linkable statik, CyclonSN sampled) {
		for (int i = 0; i < statik.degree(); i++) {
			Node neighbor = statik.getNeighbor(i);
			if (sampled.contains(neighbor) && !fSeen.get(i)) {
				fCasualHits++;
				fSeen.set(i);
			}
		}
	}

	private void proactiveQuery(Node ourNode) {
		/** Picks a peer. **/
		Node neighbor = fTabooFilter == null ? fSelector.selectPeer(ourNode)
				: fSelector.selectPeer(ourNode, fTabooFilter);
		
		if (neighbor == null) {
			return;
		}
		
		Linkable ourSn = (Linkable) ourNode.getProtocol(fStatic);
		fIndexes.resize(ourSn.degree(), false);
		
		// If he does, "contacts" the neighbor and queries for a useful IP.
		// Contact strategy depends on the mode.
		switch (fMode) {
		case PEERSAMPLING:
			linkableCollect(ourSn, (Linkable) neighbor.getProtocol(fSampled),
					fIndexes);
			break;
		case COLLECTOR:
			collectorCollect(ourNode, neighbor, fIndexes);
			break;
		}

		// Found something.
		for (int i = 0; i < fIndexes.size(); i++) {
			checkIndex(fIndexes.get(i), ourSn);
			fSeen.set(fIndexes.get(i));
			fProactiveHits++;
		}
	}

	// ----------------------------------------------------------------------
	// Collector methods.
	// ----------------------------------------------------------------------

	/**
	 * Given two neighboring nodes, computes the set of indexes in the social
	 * neighborhood of the first node that:
	 * <ol>
	 * <li>are also neighbors with the second node;</li>
	 * <li>have been seen by the neighbor's collector;</li>
	 * <li>haven't been seen by our collector.
	 * </ol>
	 * <BR>
	 * In other words, tells us the indices of the elements in our social
	 * neighborhood that have been seen by our neighbor's collector.
	 * 
	 * @param ours
	 *            a node.
	 * 
	 * @param neighbor
	 *            a neighboring node.
	 * 
	 * @param storage
	 *            a {@link StaticVector} for storing the indices, or
	 *            <code>null</code> if the client just wants a count.
	 * 
	 * @return the number of nodes fitting the criterion.
	 */
	private int collectorCollect(Node ours, Node neighbor,
			StaticVector<Integer> storage) {
		Linkable ourSn = (Linkable) ours.getProtocol(fStatic);
		Linkable neighborSn = (Linkable) neighbor.getProtocol(fStatic);
		F2FOverlayCollector neighborCollector = (F2FOverlayCollector) neighbor
				.getProtocol(CommonState.getPid());

		BitSet seen = neighborCollector.fSeen;
		// Oops, not initialized yet.
		if (seen == null) {
			return 0;
		}

		/**
		 * Note: this is more contorted than linkableCollect because we need to
		 * do an expensive index mapping operation before we can assert whether
		 * our neighbor's collector has seen
		 */
		// From the set of neighbors seen by our neighbor...
		for (int i = seen.nextSetBit(0); i >= 0; i = seen.nextSetBit(i + 1)) {
			Node candidate = neighborSn.getNeighbor(i);
			// ... sees whether it is a friend of ours as well ...
			if (ourSn.contains(candidate)) {
				// ... and we haven't seen it yet.
				int idx = PeersimUtils.indexOf(candidate, ourSn);
				if (!fSeen.get(idx)) {
					checkIndex(idx, ourSn);
					storage.append(idx);
				}
			}
		}

		for (int i = 0; i < fIndexes.size(); i++) {
			checkIndex(fIndexes.get(i), ourSn);
		}

		return storage.size();
	}

	private int linkableCollect(Linkable ourSn, Linkable another,
			StaticVector<Integer> storage) {
		int degree = ourSn.degree();
		int counter = 0;

		for (int i = 0; i < degree; i++) {
			Node candidate = ourSn.getNeighbor(i);
			if (another.contains(candidate) && !fSeen.get(i)) {
				checkIndex(i, ourSn);
				if (storage != null) {
					storage.append(i);
				}
				counter++;
			}
		}

		return counter;
	}

	// ----------------------------------------------------------------------

	private void init(Linkable statik) {
		if (fSeen == null) {
			fSeen = new BitSet(statik.degree());
		}
	}

	public int achieved() {
		if (fSeen == null) {
			return -1;
		}
		return fSeen.cardinality();
	}

	public int randomHits() {
		return fCasualHits;
	}

	public int proactiveHits() {
		return fProactiveHits;
	}

	private void resetCounters() {
		fCasualHits = fProactiveHits = 0;
	}

	public Object clone() {
		try {
			F2FOverlayCollector clone = (F2FOverlayCollector) super.clone();
			if (clone.fSeen != null) {
				clone.fSeen = new BitSet(fSeen.size());
				clone.fSeen.or(fSeen);
			}
			clone.fSelector = clone.configureSelector();
			return clone;
		} catch (CloneNotSupportedException ex) {
			throw new RuntimeException(ex);
		}
	}
	
	private void checkIndex(int i, Linkable ourSn) {
		if (i >= ourSn.degree()) {
			throw new IllegalStateException();
		}
		
		Linkable theReal = (Linkable) CommonState.getNode().getProtocol(fStatic);
		if (i >= theReal.degree()) {
			throw new IllegalStateException("The real.");
		}
	}
	
	/**
	 * Selects the first node in our peer sampling view which has neighbors that
	 * are shared with our neighborhood, and that we haven't yet seen.
	 * 
	 * @author giuliano
	 */
	class FirstFitSelector implements IPeerSelector {

		private final boolean fShuffle;

		private final PermutingCache fCache;

		public FirstFitSelector(boolean shuffle) {
			fCache = new PermutingCache(fSampled);
			fShuffle = shuffle;
		}

		@Override
		public Node selectPeer(Node source, ISelectionFilter filter) {
			Linkable ourSn = (Linkable) source.getProtocol(fStatic);
			Linkable searchSn;
			if (fShuffle) {
				fCache.populate(source);
				fCache.shuffle();
				searchSn = fCache.asLinkable();
			} else {
				searchSn = ourSn;
			}

			// Picks the first neighbor that shares something with us.
			int degree = searchSn.degree();
			for (int i = 0; i < degree; i++) {
				Node neighbor = searchSn.getNeighbor(i);
				Linkable neighborSn = (Linkable) neighbor.getProtocol(fStatic);
				if (F2FOverlayCollector.this.linkableCollect(ourSn, neighborSn,
						null) != -1) {
					return neighbor;
				}
			}

			return null;
		}

		@Override
		public boolean supportsFiltering() {
			return true;
		}

		@Override
		public Node selectPeer(Node source) {
			return selectPeer(source, ISelectionFilter.ALWAYS_TRUE_FILTER);
		}

		@Override
		public void clear(Node source) {
		}
	}

	private class IntersectingUnseenUtility implements IUtilityFunction {

		@Override
		public int utility(Node base, Node target) {
			Linkable ourSn = (Linkable) base.getProtocol(fStatic);
			Linkable neighborSn = (Linkable) target.getProtocol(fStatic);
			return F2FOverlayCollector.this.linkableCollect(ourSn, neighborSn,
					null);
		}

		@Override
		public boolean isDynamic() {
			return true;
		}

		public Object clone() {
			return null;
		}
	}

}
