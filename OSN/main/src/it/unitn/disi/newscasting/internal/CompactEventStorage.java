package it.unitn.disi.newscasting.internal;

import it.unitn.disi.epidemics.BaseGossipMessage;
import it.unitn.disi.epidemics.IGossipMessage;
import it.unitn.disi.epidemics.IWritableEventStorage;
import it.unitn.disi.newscasting.IMessageVisibility;
import it.unitn.disi.utils.peersim.SNNode;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import peersim.core.Linkable;
import peersim.core.Node;

/**
 * {@link CompactEventStorage} can efficiently store {@link IGossipMessage}s,
 * but will only preserve the sequence numbers and the originator. This means
 * that {@link #add(Node, int)} operations can accept any subclass of
 * {@link IGossipMessage}, but semantics for {@link #tweetsFor(Node)} and
 * {@link #contains(IGossipMessage)} are different from expected.
 * 
 * @author giuliano
 */
public class CompactEventStorage implements IWritableEventStorage, Cloneable {

	// ----------------------------------------------------------------------

	private Map<Node, LinkedList<Integer>> fIntervals = new HashMap<Node, LinkedList<Integer>>();

	private IMessageVisibility fVisibility;

	// ----------------------------------------------------------------------

	public CompactEventStorage(IMessageVisibility visibility) {
		fVisibility = visibility;
	}

	// ----------------------------------------------------------------------

	/**
	 * Adds a new event to storage. Note that any event will be accepted by this
	 * method, even those that do not belong to the neighborhood of the node.
	 * 
	 * @param owner
	 * @param seqNumber
	 * @return
	 */
	public boolean add(Node owner, int seqNumber) {
		LinkedList<Integer> theList = getList(owner, true);
		ListIterator<Integer> it = theList.listIterator();

		boolean added = false;
		// First, finds insertion point.
		while (it.hasNext()) {
			// Is it at the left border?
			Integer start = it.next();
			if (seqNumber == (start - 1)) {
				// Checks to see if we need to merge
				// with the previous interval.
				it.remove();
				if (it.hasPrevious()) {
					if (it.previous() == seqNumber - 1) {
						it.remove();
					} else {
						it.next();
						it.add(seqNumber);
					}
				} else {
					it.add(seqNumber);
				}

				added = true;
				break;
			}

			// Right border?
			Integer end = it.next();
			if (seqNumber == (end + 1)) {
				// Checks to see if we need to merge
				// with next interval.
				it.remove();
				if (it.hasNext()) {
					if (it.next() == seqNumber + 1) {
						it.remove();
					} else {
						it.previous();
						it.add(seqNumber);
					}
				} else {
					it.add(seqNumber);
				}
				added = true;
				break;
			}

			// See if it's contained in the interval.
			if (seqNumber >= start && seqNumber <= end) {
				return false;
			}

			// None of the above. Either it's a new interval,
			// or belongs to another interval (yet to be examined).
			// Since intervals are sorted, it will only belong
			// to a new interval if it's larger larger than start.
			if (seqNumber < start) {
				it.previous();
				it.previous();
				it.add(seqNumber);
				it.add(seqNumber);
				added = true;
				break;
			}
		}

		// If interval wasn't added, then it means it's at the end.
		if (!added) {
			theList.add(seqNumber);
			theList.add(seqNumber);
		}

		return true;
	}

	// ----------------------------------------------------------------------

	private LinkedList<Integer> getList(Node owner, boolean create) {
		LinkedList<Integer> list = fIntervals.get(owner);
		if (list == null && create) {
			list = new LinkedList<Integer>();
			fIntervals.put(owner, list);
		}
		return list;
	}

	// ----------------------------------------------------------------------

	public List<Integer> eventsFor(Node owner) {
		List<Integer> list = getList(owner, false);
		if (list == null) {
			return null;
		}

		return Collections.unmodifiableList(list);
	}

	// ----------------------------------------------------------------------

	Set<Node> allKeys() {
		return Collections.unmodifiableSet(fIntervals.keySet());
	}

	// ----------------------------------------------------------------------

	@Override
	public boolean contains(IGossipMessage msg) {
		return contains(msg.originator(), msg.sequenceNumber());
	}

	// ----------------------------------------------------------------------

	@Override
	public boolean remove(Node node, int sequence) {
		throw new UnsupportedOperationException();
	}

	// ----------------------------------------------------------------------

	@Override
	public IGossipMessage retrieve(Node originator, int sequence) {
		if (this.contains(originator, sequence)) {
			return new SimpleMessage(originator, sequence, fVisibility);
		}
		return null;
	}

	// ----------------------------------------------------------------------

	public boolean contains(Node owner, int seqnumber) {
		List<Integer> list = getList(owner, false);
		if (list == null) {
			return false;
		}

		Iterator<Integer> it = list.iterator();
		while (it.hasNext()) {
			Integer start = it.next();
			Integer end = it.next();

			if (seqnumber >= start && seqnumber <= end) {
				return true;
			}
		}

		return false;
	}

	// ----------------------------------------------------------------------

	/**
	 * Performs a merge of the events known by this {@link CompactEventStorage}
	 * with the events known by some other {@link CompactEventStorage},
	 * constrained by a given {@link Linkable}. In essence, only those events
	 * produced by {@link Node} instances known to the {@link Linkable} will
	 * remain in the {@link CompactEventStorage} after the operation.<BR>
	 * <BR>
	 * Note that this operation affects only this {@link CompactEventStorage},
	 * the other will remain unchanged (so technically it's not a merge).
	 * 
	 * @param sender
	 *            the 'sending' node; that is, the node which owns the
	 *            {@link CompactEventStorage} object being merged into this one.
	 * @param receiver
	 *            the 'receiving' node; that is, the node which owns this
	 *            {@link CompactEventStorage} object.
	 * @param other
	 *            the {@link CompactEventStorage} instance with which we want to
	 *            merge.
	 * @param observer
	 *            a {@link IMergeObserver} interested in knowing what's going on
	 *            with the merge.
	 * @param neighborhood
	 *            events produced by nodes not contained in this neighborhood
	 *            will be removed from storage.
	 * 
	 */
	public void merge(Node sender, Node receiver, CompactEventStorage other,
			IMergeObserver observer, Linkable neighborhood) {
		Iterator<Node> keyIt = fIntervals.keySet().iterator();
		while (keyIt.hasNext()) {
			Node key = keyIt.next();

			// Performs garbage collection. We no longer have
			// this node in our social network.
			if (!neighborhood.contains(key) && (receiver != null)
					&& (key.getID() != receiver.getID())) {
				keyIt.remove();
				continue;
			}

			/** Merges occur only for keys in common. */
			if (!other.fIntervals.containsKey(key)) {
				continue;
			}

			// Simulates sending the digest.
			observer.sendDigest(sender, receiver, key, eventsFor(key));

			// Performs the actual merge.
			fIntervals.put(key, this.mergeIntervals(sender, receiver, key,
					this.getList(key, false), other.getList(key, false),
					observer, neighborhood));
		}

		this.deltaAdd(sender, receiver, other, observer, neighborhood);
	}

	// ----------------------------------------------------------------------

	private LinkedList<Integer> mergeIntervals(Node sender, Node receiver,
			Node key, LinkedList<Integer> l1, LinkedList<Integer> l2,
			IMergeObserver observer, Linkable socialNeighborhood) {

		// TODO do it in-place.
		LinkedList<Integer> merged = new LinkedList<Integer>();
		ListIterator<Integer> it1 = l1.listIterator();
		ListIterator<Integer> it2 = l2.listIterator();

		int[] interval = new int[2];
		int last = Integer.MIN_VALUE;

		// Proceeds by picking the block
		// with the smallest start.
		while (it1.hasNext() || it2.hasNext()) {
			nextSmallest(it1, it2, interval);

			// Already contained in the merge.
			if (interval[1] < last) {
				continue;
			}

			// It's a new interval alltogether.
			if (interval[0] > (last + 1)) {
				merged.add(interval[0]);
			} else {
				merged.removeLast();
			}

			// In any case, it always ends at the end of the
			// currently picked interval.
			merged.add(interval[1]);
			last = merged.peekLast();
		}

		// Now notifies the observer about the merges.
		it1 = l1.listIterator();
		it2 = merged.listIterator();

		while (it2.hasNext()) {
			// Picks a large block.
			int start = it2.next();
			int end = it2.next();

			// Goes through the original list,
			// reporting the holes.
			while (it1.hasNext()) {
				int originalStart = it1.next();

				if (originalStart > end) {
					it1.previous();
					break;
				} else if (originalStart - start > 0) {
					deliver(observer, sender, receiver, key, start,
							originalStart - 1);
				}

				start = it1.next() + 1;
			}

			// Reports an eventual tail.
			if (start <= end) {
				deliver(observer, sender, receiver, key, start, end);
			}
		}

		return merged;
	}

	// ----------------------------------------------------------------------

	private int nextSmallest(ListIterator<Integer> it1,
			ListIterator<Integer> it2, int[] interval) {
		int start1 = Integer.MAX_VALUE;
		int start2 = Integer.MAX_VALUE;

		if (it1.hasNext()) {
			start1 = it1.next();
		}

		if (it2.hasNext()) {
			start2 = it2.next();
		}

		if (start1 < start2) {
			interval[0] = start1;
			interval[1] = it1.next();

			// Rollsback the other.
			if (start2 != Integer.MAX_VALUE) {
				it2.previous();
			}

			return 1;
		} else {
			interval[0] = start2;
			interval[1] = it2.next();

			// Rollsback the other.
			if (start1 != Integer.MAX_VALUE) {
				it1.previous();
			}

			return 2;
		}

	}

	// ----------------------------------------------------------------------

	private void deltaAdd(Node sender, Node receiver,
			CompactEventStorage other, IMergeObserver observer,
			Linkable socialNeighborhood) {
		for (Node key : other.fIntervals.keySet()) {

			// If we have already merged those before, or the
			// node isn't in our neighborhood, continue.
			if (fIntervals.containsKey(key)
					|| !socialNeighborhood.contains(key)) {
				continue;
			}

			LinkedList<Integer> list = other.getList(key, false);
			fIntervals.put(key, new LinkedList<Integer>(list));

			// Now notifies about the updates.
			Iterator<Integer> it = list.iterator();
			while (it.hasNext()) {
				deliver(observer, sender, receiver, key, it.next(), it.next());
			}
		}
	}

	// ----------------------------------------------------------------------

	private void deliver(IMergeObserver observer, Node sender, Node receiver,
			Node key, int start, int end) {

		for (int i = start; i <= end; i++) {
			observer.delivered((SNNode) sender, (SNNode) receiver,
					new SimpleMessage((SNNode) key, i, fVisibility), false);
		}
	}

	// ----------------------------------------------------------------------

	public void consistencyCheck() {
		for (List<Integer> list : fIntervals.values()) {

			Iterator<Integer> it = list.iterator();
			int last = Integer.MIN_VALUE + 1;
			while (it.hasNext()) {
				int start = it.next();
				int end = it.next();

				if (start <= (last - 1) || start > end) {
					System.err.println(list);
					throw new AssertionError();
				}

				last = end;
			}
		}
	}

	// ----------------------------------------------------------------------

	@Override
	public Iterator<IGossipMessage> tweetsFor(Node node) {
		return new TweetIterator(node);
	}

	// ----------------------------------------------------------------------

	@Override
	public boolean add(IGossipMessage msg) {
		return this.add(msg.originator(), msg.sequenceNumber());
	}

	// ----------------------------------------------------------------------

	public int distinctNodes() {
		return fIntervals.size();
	}

	// ----------------------------------------------------------------------

	@Override
	public Set<Node> nodes() {
		return Collections.unmodifiableSet(fIntervals.keySet());
	}

	// ----------------------------------------------------------------------

	@Override
	public void clear() {
		fIntervals.clear();
	}

	// ----------------------------------------------------------------------

	public int elements() {
		int total = 0;
		for (LinkedList<Integer> list : fIntervals.values()) {
			total += list.size();
		}

		return total;
	}

	// ----------------------------------------------------------------------

	public Object clone() {
		try {
			CompactEventStorage cloned = (CompactEventStorage) super.clone();
			cloned.fIntervals = new HashMap<Node, LinkedList<Integer>>();

			for (Node key : fIntervals.keySet()) {
				cloned.fIntervals.put(key,
						new LinkedList<Integer>(fIntervals.get(key)));
			}

			return cloned;
		} catch (CloneNotSupportedException ex) {
			throw new RuntimeException(ex);
		}
	}

	// ----------------------------------------------------------------------

	private class TweetIterator implements Iterator<IGossipMessage> {

		private Node fNode;

		private Iterator<Integer> fIntervalIterator;

		private int fCurrent = 0;

		private int fNext = -1;

		public TweetIterator(Node node) {
			if (node == null) {
				return;
			}

			fIntervalIterator = fIntervals.get(node).iterator();
			nextInterval();
		}

		@Override
		public boolean hasNext() {
			return fCurrent < fNext;
		}

		@Override
		public SimpleMessage next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}
			SimpleMessage next = new SimpleMessage(fNode, fCurrent++,
					fVisibility);
			if (fCurrent == fNext) {
				nextInterval();
			}
			return next;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		private void nextInterval() {
			if (fIntervalIterator.hasNext()) {
				fCurrent = fIntervalIterator.next();
				fNext = fIntervalIterator.next();
			}
		}
	}

	static class SimpleMessage extends BaseGossipMessage {

		protected SimpleMessage(Node originator, int sequence,
				IMessageVisibility vis) {
			super(originator, sequence, vis);
		}

	}
}
