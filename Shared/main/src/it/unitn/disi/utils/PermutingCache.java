package it.unitn.disi.utils;

import peersim.core.CommonState;
import peersim.core.Linkable;
import peersim.core.Node;

public class PermutingCache implements IExchanger, Cloneable {

	private static Node [] fInternalCache = new Node[2000];
	
	private final int fLinkableId;
	
	private int fSize = -1;
	
	public PermutingCache(int linkableId) {
		fLinkableId = linkableId;
	}
	
	public void shuffle(Node source) {
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

		fSize = MiscUtils.compact(fInternalCache, this, size);
		OrderingUtils.permute(0, size, this, CommonState.r);
	}
	
	public int size() {
		return fSize;
	}
	
	public Node get(int i) {
		return fInternalCache[i];
	}
	
	public boolean contains(Node target) {
		for (Node node : fInternalCache) {
			if (node.equals(target)) {
				return true;
			}
		}
		
		return false;
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
