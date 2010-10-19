package it.unitn.disi.utils.peersim;

import it.unitn.disi.ISelectionFilter;
import it.unitn.disi.utils.IReference;
import it.unitn.disi.utils.OrderingUtils;
import it.unitn.disi.utils.collections.IExchanger;

import java.util.Arrays;
import java.util.Comparator;

import peersim.core.CommonState;
import peersim.core.Linkable;
import peersim.core.Node;

/**
 * {@link PermutingCache} provides operations for making local copies of
 * {@link Linkable} contents, so they can be manipulated. All
 * {@link PermutingCache} instances share a single common internal buffer,
 * making it memory efficient.
 * 
 * @author giuliano
 */
public class PermutingCache implements IExchanger, Cloneable {

	// ----------------------------------------------------------------------
	// Shared (static) state.
	// ----------------------------------------------------------------------
	/**
	 * Manipulation buffer.
	 */
	private static Node[] fInternalCache = new Node[2000];

	/**
	 * Size of the content that's been copied in here.
	 */
	private static int fSize = -1;

	// ----------------------------------------------------------------------
	// Parameter storage.
	// ----------------------------------------------------------------------
	/**
	 * ID of the linkable.
	 */
	private final IReference<Linkable> fLinkable;

	/**
	 * Creates a new {@link PermutingCache}, binding it to a linkable id.
	 * 
	 * @param linkableId
	 */
	public PermutingCache(int linkableId) {
		this(new ProtocolReference<Linkable>(linkableId));
	}
	
	public PermutingCache(IReference<Linkable> linkable) {
		fLinkable = linkable;
	}

	/**
	 * Convenience method. Equivalent to:<BR>
	 * <BR>
	 * 
	 * <code> this.populate(source, ISelectionFilter.ALWAYS_TRUE_FILTER);</code>
	 */
	public void populate(Node source) {
		this.populate(source, ISelectionFilter.ALWAYS_TRUE_FILTER);
	}

	/**
	 * Loads the content of the bound {@link Linkable} into the
	 * {@link PermutingCache}, subject to an {@link ISelectionFilter}. Should be
	 * always called before a manipulations begin.
	 * 
	 * @param source
	 *            the node holding the {@link Linkable} instance.
	 * 
	 * @param filter
	 *            an {@link ISelectionFilter} that dictates which neighbors will
	 *            actually get transferred into the manipulation buffer.
	 * 
	 */
	public void populate(Node source, ISelectionFilter filter) {
		Linkable linkable = fLinkable.get(source);
		int size = linkable.degree();
		if (size == 0) {
			fSize = 0;
			return;
		}

		if (fInternalCache.length < size) {
			fInternalCache = new Node[(int) Math.abs(Math.pow(2.0, Math
					.ceil(Math.log(size) / Math.log(2.0))))];
		}

		// Copies "viable" neighbors into cache.
		int k = 0;
		for (int i = 0; i < size; i++) {
			Node neighbor = linkable.getNeighbor(i);
			if (neighbor != null && filter.canSelect(neighbor)) {
				fInternalCache[k++] = neighbor;
			} 
		}
		
		fSize = k;
	}

	/**
	 * Produces a permutation of the cached content. Permutations are generated
	 * uniformly at random.
	 */
	public void shuffle() {
		check();
		this.shuffle(0, fSize);
	}

	/**
	 * Produces a permutation of a subset of the cached content. Permutations
	 * are generated uniformly at random.
	 */
	public void shuffle(int start, int end) {
		check();
		OrderingUtils.permute(start, end, this, CommonState.r);
	}

	/**
	 * Ranks the cached elements according to the ordering induced by a
	 * {@link Comparator}.
	 */
	public void orderBy(Comparator<Node> comparator) {
		check();
		Arrays.sort(fInternalCache, 0, fSize, comparator);
	}

	/**
	 * @return the current number of cached elements. 
	 */
	public int size() {
		check();
		return fSize;
	}

	/**
	 * Returns an element.
	 * 
	 * @param i
	 *            the element index to return.
	 * @return the i-th cached element.
	 */
	public Node get(int i) {
		check();
		return fInternalCache[i];
	}

	public boolean contains(Node target) {
		check();
		for (Node node : fInternalCache) {
			if (node.equals(target)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Invalidates the current cache. Should always be called after clients are
	 * done with their manipulations.
	 */
	public void invalidate() {
		fSize = -1;
	}

	private void check() {
		if (fSize == -1) {
			throw new IllegalStateException();
		}
	}
	
	public void exchange(int i, int j) {
		Node temp = fInternalCache[i];
		fInternalCache[i] = fInternalCache[j];
		fInternalCache[j] = temp;
	}

	public Object clone() {
		// We're immutable, so we can return ourselves.
		return this;
	}
	
	// ----------------------------------------------------------------------
	// Linkable view for permuting cache.
	// ----------------------------------------------------------------------
	
	private final Linkable fLinkableView = new Linkable() {

		@Override
		public int degree() {
			return PermutingCache.this.size();
		}

		@Override
		public Node getNeighbor(int i) {
			return PermutingCache.this.get(i);
		}

		@Override
		public boolean contains(Node neighbor) {
			return PermutingCache.this.contains(neighbor);
		}
		
		@Override
		public boolean addNeighbor(Node neighbour) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void onKill() { }

		@Override
		public void pack() { }
	
	};
	
	public Linkable asLinkable() {
		return fLinkableView;
	}
}
