package it.unitn.disi.utils;

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
	
	// ----------------------------------------------------------------------
	// Parameter storage.
	// ----------------------------------------------------------------------
	/**
	 * ID of the linkable.
	 */
	private final int fLinkableId;

	// ----------------------------------------------------------------------
	// Size of the last content. 
	// ----------------------------------------------------------------------
	private int fSize = -1;
	
	public PermutingCache(int linkableId) {
		fLinkableId = linkableId;
	}

	public void populate(Node source) {
		
		Linkable linkable = (Linkable) source.getProtocol(fLinkableId);
		int size = linkable.degree();
		if (size == 0) {
			return;
		}

		if (fInternalCache.length < size) {
			fInternalCache = new Node[(int) Math.abs(Math.pow(2.0, Math
					.ceil(Math.log(size) / Math.log(2.0))))];
		}

		// Copies neighbors into cache.
		for (int i = 0; i < size; i++) {
			fInternalCache[i] = linkable.getNeighbor(i);
		}

		// Compacts because getNeighbor might have returned null.
		fSize = MiscUtils.compact(fInternalCache, this, size);
	}

	public void shuffle() {
		check();
		this.shuffle(0, fSize);
	}

	public void shuffle(int start, int end) {
		check();
		OrderingUtils.permute(start, end, this, CommonState.r);
	}

	public void orderBy(Comparator<Node> comparator) {
		check();
		Arrays.sort(fInternalCache, 0, fSize, comparator);
	}

	public int size() {
		check();
		return fSize;
	}

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
	
	public void invalidate() {
		fSize = -1;
	}

	private void check() {
		if(fSize == -1) {
			throw new IllegalStateException();
		}
	}

	public void exchange(int i, int j) {
		Node temp = fInternalCache[i];
		fInternalCache[i] = fInternalCache[j];
		fInternalCache[j] = temp;
	}

	public Object clone() {
		try {
			PermutingCache cloned = (PermutingCache) super.clone();
			return cloned;
		} catch (CloneNotSupportedException ex) {
			throw new RuntimeException(ex);
		}
	}
}
