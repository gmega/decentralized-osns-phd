package it.unitn.disi.utils.peersim;

import java.util.BitSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

import peersim.core.Linkable;
import peersim.core.Node;

/**
 * {@link BitSetNeighborhood} is a {@link Linkable} decorator which allows
 * elements to be masked in and out using {@link BitSet}s. The rationale is that
 * this should be faster and lighter than using full-blown arrays to perform the
 * same task.
 * 
 * @author giuliano
 */
public class BitSetNeighborhood implements Linkable, Iterator<Node> {

	private static final int UNKNOWN = -2;

	private final Linkable fSource;

	private final BitSet fMembers;

	private int fLastRequested;

	private int fIteratorHint;

	public BitSetNeighborhood(Linkable source) {
		this(source, false);
	}

	public BitSetNeighborhood(BitSetNeighborhood other) {
		fSource = other.fSource;
		fMembers = new BitSet();
		fMembers.or(other.fMembers);
		fLastRequested = other.fLastRequested;
		fIteratorHint = other.fIteratorHint;
	}

	public BitSetNeighborhood(Linkable source, boolean skipChecks) {
		if (!skipChecks && !isSorted(source)) {
			throw new IllegalArgumentException(
					"Neighborhoods must be sorted by ID.");
		}
		fSource = source;
		fMembers = new BitSet();
	}

	// ------------------------------------------------------------------------
	// Linkable interface.
	// ------------------------------------------------------------------------

	@Override
	public int degree() {
		return fMembers.cardinality();
	}

	/**
	 * This implementation supports O(1) (amortized) cost for <i>n</i> access
	 * operations only if iteration is performed sequentially. If performed
	 * out-of-order, the cost of <i>n</i> accesses will be
	 * O(<i>n<sup>2</sup>)</i>.
	 * 
	 * @see Linkable#getNeighbor(int)
	 */
	@Override
	public Node getNeighbor(int i) {
		if (fLastRequested != (i - 1) || fIteratorHint == UNKNOWN) {
			fLastRequested = -1;
			fIteratorHint = -1;
		}

		int idx = fIteratorHint;
		for (int j = fLastRequested + 1; j <= i; j++) {
			idx = fMembers.nextSetBit(idx + 1);
			if (idx == -1) {
				throw new ArrayIndexOutOfBoundsException(i);
			}
		}

		fLastRequested = i;
		fIteratorHint = idx;
		return fSource.getNeighbor(idx);
	}

	@Override
	public boolean addNeighbor(Node neighbour) {
		return addNeighbor(neighbour, false);
	}
	
	public boolean addNeighbor(Node neighbour, boolean failSilently) {
		int idx = PeersimUtils.indexOf(neighbour, fSource);
		if (idx == -1) {
			if (!failSilently) {
				throw new IllegalArgumentException("Node " + neighbour.getID()
						+ " can't be a part of this neighborhood.");
			} else {
				return false;
			}
		}

		boolean contains = fMembers.get(idx);
		if (!contains) {
			fMembers.set(idx);
			reset();
		}

		return contains;
	}

	@Override
	public boolean contains(Node neighbor) {
		int idx = PeersimUtils.indexOf(neighbor, fSource);
		if (idx == -1) {
			return false;
		}
		return fMembers.get(idx);
	}

	@Override
	public void onKill() {
	}

	@Override
	public void pack() {
	}

	// ------------------------------------------------------------------------
	// Iterator interface.
	// ------------------------------------------------------------------------

	@Override
	public boolean hasNext() {
		if (!(fLastRequested == -1)) {
			reset();
		}
		return fMembers.nextSetBit(fIteratorHint + 1) != -1;
	}

	@Override
	public Node next() {
		if (!hasNext()) {
			throw new NoSuchElementException();
		}

		int idx = fMembers.nextSetBit(fIteratorHint);
		fIteratorHint = idx;
		return fSource.getNeighbor(idx);
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	public void reset() {
		fLastRequested = -1;
		fIteratorHint = UNKNOWN;
	}

	// ------------------------------------------------------------------------
	// Other public methods.
	// ------------------------------------------------------------------------

	/**
	 * Allows two {@link BitSetNeighborhood}s to be merged in linear time even
	 * if their underlying {@link Linkable}s differ.
	 */
	public int addAll(BitSetNeighborhood other) {
		int merged = degree();
		if (other.sameSource(this)) {
			fastMerge(other);
		} else {
			linearMerge(other);
		}
		reset();
		return degree() - merged;
	}

	/**
	 * @param other
	 *            performs the set difference between the set of members in this
	 *            {@link BitSetNeighborhood} and the set of members in the
	 *            neighborhood given as a parameter.
	 */
	public void setDifference(BitSetNeighborhood other) {
		fMembers.or(other.fMembers);
		fMembers.xor(other.fMembers);
	}

	public void removeAll() {
		fMembers.clear();
		reset();
	}

	/**
	 * @return the underlying {@link Linkable} -- assumed to be immutable --
	 *         over which the {@link BitSetNeighborhood} operates.
	 */
	public Linkable linkable() {
		return fSource;
	}

	private boolean sameSource(BitSetNeighborhood other) {
		return fSource.equals(other.fSource);
	}

	private void fastMerge(BitSetNeighborhood other) {
		fMembers.or(other.fMembers);
	}

	private void linearMerge(BitSetNeighborhood other) {
		int s1 = fSource.degree(), s2 = other.fSource.degree();
		int k = 0, j = 0;
		while (j < s1 && k < s2) {
			if (nid(j) < other.nid(k)) {
				j++;
			} else if (nid(j) > other.nid(k)) {
				k++;
			} else {
				if (other.fMembers.get(k)) {
					fMembers.set(j);
				}
				k++;
				j++;
			}
		}
	}

	private long nid(int index) {
		return fSource.getNeighbor(index).getID();
	}

	// ------------------------------------------------------------------------
	// Private helpers.
	// ------------------------------------------------------------------------

	private boolean isSorted(Linkable source) {
		int degree = source.degree();
		long previous = Long.MIN_VALUE;
		for (int i = 0; i < degree; i++) {
			Node neighbor = source.getNeighbor(i);
			if (neighbor.getID() < previous) {
				return false;
			}
			previous = neighbor.getID();
		}
		return true;
	}
	
	@Override
	public String toString() {
		return fMembers.toString();
	}

}
