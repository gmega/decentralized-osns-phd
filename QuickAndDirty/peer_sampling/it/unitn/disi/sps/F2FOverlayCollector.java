package it.unitn.disi.sps;

import it.unitn.disi.ISelectionFilter;
import it.unitn.disi.newscasting.IPeerSelector;
import it.unitn.disi.newscasting.internal.selectors.PercentileCentralitySelector;
import it.unitn.disi.newscasting.internal.selectors.IUtilityFunction;
import it.unitn.disi.sps.selectors.TabooSelectionFilter;
import it.unitn.disi.utils.IReference;
import it.unitn.disi.utils.collections.StaticVector;
import it.unitn.disi.utils.peersim.FallThroughReference;
import it.unitn.disi.utils.peersim.IInitializable;
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
public class F2FOverlayCollector implements CDProtocol, Linkable, IInitializable {

	private static final StaticVector<Integer> fIndexes = new StaticVector<Integer>();

	static enum Layer {
		PEERSAMPLING, COLLECTOR;
	}

	static enum ProactiveSelection {
		PASSIVE, FIRSTFIT, SHUFFLING, HIGHESTRANKING;
	}

	/**
	 * Utility functions.
	 * 
	 * @author giuliano
	 * 
	 */
	static enum UtilityFunction {
		LOCAL, ORACLE
	}

	// ----------------------------------------------------------------------
	// Parameters.
	// ----------------------------------------------------------------------

	@Attribute("social_neighbourhood")
	private int fStatic;

	@Attribute("sps")
	private int fSampled;

	@Attribute(value = "log_hits")
	boolean fLogHits;
	
	private Layer fSelection;

	private Layer fExchange;

	private ProactiveSelection fSelectionMode;

	private UtilityFunction fUtilityFunction;
	
	private int fSentRounds = 0;

	// ----------------------------------------------------------------------
	// State.
	// ----------------------------------------------------------------------

	private TabooSelectionFilter fTabooFilter;

	private IPeerSelector fSelector;

	private BitSet fSeen;

	private Node fNode;

	private int fCasualHits;

	private int fProactiveHits;

	public F2FOverlayCollector(
			@Attribute("selection_layer") String selectionLayer,
			@Attribute("exchange_layer") String exchangeLayer,
			@Attribute("selection_mode") String selectionMode,
			@Attribute(value = "utility_function", defaultValue = "LOCAL") String utility,
			@Attribute(value = "taboo", defaultValue = "0") int tabooSize) {
		fSelection = Layer.valueOf(selectionLayer.toUpperCase());
		fExchange = Layer.valueOf(exchangeLayer.toUpperCase());
		fSelectionMode = ProactiveSelection
				.valueOf(selectionMode.toUpperCase());
		fUtilityFunction = UtilityFunction.valueOf(utility.toUpperCase());

		if (tabooSize > 0) {
			fTabooFilter = new TabooSelectionFilter(tabooSize);
		}
		fSelector = selector();
	}

	private IPeerSelector selector() {

		IReference<Linkable> neighborhood;
		if (fSelection == Layer.PEERSAMPLING) {
			neighborhood = new ProtocolReference<Linkable>(fSampled);
		} else {
			neighborhood = new FallThroughReference<Linkable>(this);
		}

		IPeerSelector selector = null;

		switch (fSelectionMode) {
		case HIGHESTRANKING:
			selector = new PercentileCentralitySelector(neighborhood,
					new FallThroughReference<IUtilityFunction>(
							utilityFunction()), 1.0, CommonState.r);
			break;
		case FIRSTFIT:
			selector = new FirstFitSelector(false, neighborhood);
			break;
		case SHUFFLING:
			selector = new FirstFitSelector(true, neighborhood);
			break;
		}
		return selector;
	}

	private IUtilityFunction utilityFunction() {
		switch (fUtilityFunction) {
		case LOCAL:
			return new IntersectingUnseenUtility();
		case ORACLE:
			return new OracleMaximumGainUtility();
		}

		throw new IllegalArgumentException(fUtilityFunction.toString());
	}

	public void initialize(Node node) {
		fNode = node;
		Linkable statik = statik(node);
		init(statik);
	}
	
	public void reinitialize() { }

	@Override
	public void nextCycle(Node node, int protocolID) {
		if (node != fNode) {
			throw new IllegalStateException();
		}

		Linkable statik = statik(node);
		Linkable sampled = sampled(node);

		// Clear static cached data.
		fIndexes.clear();
		fIterationCache.clear();

		resetCounters();

		// Collect friends from the peer sampling layer (if any).
		collectFriends(node, statik, sampled);

		// Proactively tries to obtain new entries.
		if (fSelectionMode != ProactiveSelection.PASSIVE) {
			proactiveQuery(node);
		}
	}

	private void collectFriends(Node ours, Linkable statik, Linkable sampled) {
		for (int i = 0; i < statik.degree(); i++) {
			Node neighbor = statik.getNeighbor(i);
			if (sampled.contains(neighbor) && !fSeen.get(i)) {
				fCasualHits++;
				markSeen(i, ours, statik);
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

		int received = 0;		
		Linkable ourSn = statik(ourNode);
		fIndexes.resize(ourSn.degree(), false);
		// If he does, "contacts" the neighbor and queries for a useful IP.
		// Exchange strategy depends on the mode.
		switch (fExchange) {
		case COLLECTOR:
			received += collectorCollect(ourNode, neighbor, fIndexes);
			markSeen(fIndexes, ourNode, ourSn);
			fIndexes.clear();
		
		case PEERSAMPLING:
			received += linkableCollect(ourSn, sampled(neighbor), fIndexes);
			markSeen(fIndexes, ourNode, ourSn);
			break;
		}

		fSentRounds++;
	}

	private void markSeen(StaticVector<Integer> indexes, Node ourNode,
			Linkable ourSn) {
		// Found something.
		for (int i = 0; i < fIndexes.size(); i++) {
			markSeen(indexes.get(i), ourNode, ourSn);
			fProactiveHits++;
		}
	}

	private void markSeen(int i, Node node, Linkable statik) {
		checkIndex(i, statik);
		if (!fSeen.get(i) && fLogHits) {
			System.out.println("L: " + node.getID() + " "
					+ statik.getNeighbor(i).getID() + " "
					+ CommonState.getTime());
		}
		fSeen.set(i);
	}

	private Linkable sampled(Node node) {
		return (Linkable) node.getProtocol(fSampled);
	}

	private Linkable statik(Node node) {
		return (Linkable) node.getProtocol(fStatic);
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
		Linkable ourSn = statik(ours);
		Linkable neighborSn = statik(neighbor);
		F2FOverlayCollector neighborCollector = (F2FOverlayCollector) neighbor
				.getProtocol(CommonState.getPid());

		BitSet seen = neighborCollector.fSeen;
		/**
		 * Note: this is more contorted than linkableCollect because we need to
		 * do an expensive index mapping operation before we can assert whether
		 * our neighbor's collector has seen a neighbor of ours or not.
		 */
		int counter = 0;
		// From the set of neighbors seen by our neighbor...
		for (int i = seen.nextSetBit(0); i >= 0; i = seen.nextSetBit(i + 1)) {
			Node candidate = neighborSn.getNeighbor(i);
			// ... sees whether it is a friend of ours as well ...
			if (ourSn.contains(candidate)) {
				// ... and we haven't seen it yet.
				int idx = PeersimUtils.indexOf(candidate, ourSn);
				if (!fSeen.get(idx)) {
					counter++;
					if (storage != null) {
						storage.append(idx);
					}
				}
			}
		}

		return counter;
	}

	private int linkableCollect(Linkable ourSn, Linkable another,
			StaticVector<Integer> storage) {
		int degree = ourSn.degree();
		int counter = 0;

		for (int i = 0; i < degree; i++) {
			Node candidate = ourSn.getNeighbor(i);
			if (another.contains(candidate) && !fSeen.get(i)) {
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

	// ----------------------------------------------------------------------

	public boolean seen(int index) {
		return fSeen.get(index);
	}

	// ----------------------------------------------------------------------

	public int achieved() {
		return fSeen.cardinality();
	}

	// ----------------------------------------------------------------------

	public int randomHits() {
		return fCasualHits;
	}

	// ----------------------------------------------------------------------

	public int proactiveHits() {
		return fProactiveHits;
	}
	
	// ----------------------------------------------------------------------

	private void resetCounters() {
		fCasualHits = fProactiveHits = 0;
	}

	// ----------------------------------------------------------------------

	private void checkIndex(int i, Linkable ourSn) {
		if (i >= ourSn.degree()) {
			throw new IllegalStateException();
		}

		Linkable theReal = (Linkable) CommonState.getNode()
				.getProtocol(fStatic);
		if (i >= theReal.degree()) {
			throw new IllegalStateException("The real.");
		}
	}

	// ----------------------------------------------------------------------

	public Object clone() {
		try {
			F2FOverlayCollector clone = (F2FOverlayCollector) super.clone();
			if (clone.fSeen != null) {
				clone.fSeen = new BitSet(fSeen.size());
				clone.fSeen.or(fSeen);
			}
			clone.fSelector = clone.selector();
			return clone;
		} catch (CloneNotSupportedException ex) {
			throw new RuntimeException(ex);
		}
	}

	// ----------------------------------------------------------------------
	// Linkable interface.
	// ----------------------------------------------------------------------

	private final static StaticVector<Node> fIterationCache = new StaticVector<Node>();

	private static Node fOwner = null;

	private static long fChangeTime = -1;

	/*
	 * XXX The linkable interface for F2FOverlayCollector has severe performance
	 * penalties, but at least traversals are guaranteed linear.
	 */

	@Override
	public int degree() {
		cacheInit(fNode);
		return fIterationCache.size();
	}

	@Override
	public Node getNeighbor(int i) {
		cacheInit(fNode);
		return fIterationCache.get(i);
	}

	@Override
	public boolean contains(Node node) {
		cacheInit(fNode);
		return fIterationCache.contains(node);
	}

	@Override
	public boolean addNeighbor(Node neighbour) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void pack() {
	}

	@Override
	public void onKill() {
		fSeen = null;
		fSelector = null;
	}

	private void cacheInit(Node node) {
		long time = CommonState.getTime();
		if (node.equals(fOwner) && fChangeTime == time) {
			return;
		}

		fOwner = node;
		fChangeTime = time;

		fIterationCache.clear();
		if (fSeen == null) {
			return;
		}
		Linkable sampled = sampled(node);
		fIterationCache.resize(fSeen.cardinality() + sampled.degree(), false);

		// Adds the sampled neighborhood to the cache.
		int degree = sampled.degree();
		for (int i = 0; i < degree; i++) {
			fIterationCache.append(sampled.getNeighbor(i));
		}

		// And then the nodes in the collector view.
		Linkable statik = statik(node);
		for (int i = fSeen.nextSetBit(0); i >= 0; i = fSeen.nextSetBit(i + 1)) {
			fIterationCache.append(statik.getNeighbor(i));
		}
	}

	// ----------------------------------------------------------------------

	/**
	 * Selects the first node in our peer sampling view which has neighbors that
	 * are shared with our neighborhood, and that we haven't yet seen.
	 * 
	 * @author giuliano
	 */
	class FirstFitSelector implements IPeerSelector {

		private final boolean fShuffle;

		private final PermutingCache fCache;

		public FirstFitSelector(boolean shuffle,
				IReference<Linkable> linkableRef) {
			fCache = new PermutingCache(linkableRef);
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

	private class IntersectingUnseenUtility implements IUtilityFunction<Node, Node> {

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

	/**
	 * Cheating utility function which looks into the receiving peer's cache
	 * before actual selection
	 * 
	 * @author giuliano
	 */
	private class OracleMaximumGainUtility implements IUtilityFunction<Node, Node> {

		@Override
		public int utility(Node base, Node target) {
			Linkable statik = statik(base);
			Linkable sampled = sampled(target);
			return F2FOverlayCollector.this.linkableCollect(statik, sampled,
					null)
					+ F2FOverlayCollector.this.collectorCollect(base, target,
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
