package it.unitn.disi.newscasting.internal.demers;

import it.unitn.disi.epidemics.IGossipMessage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;

import peersim.core.Linkable;
import peersim.core.Node;

/**
 * Rumor list is an auxiliary object which helps the rumor mongering protocol
 * maintain and update its list of rumors.
 */
public abstract class RumorList {

	/**
	 * The rumors we are currently transmitting.
	 */
	private LinkedList<IGossipMessage> fHotRumors = new LinkedList<IGossipMessage>();
	private List<IGossipMessage> fRoHotRumors = Collections
			.unmodifiableList(fHotRumors);

	/**
	 * See {@link DemersRumorMonger#PAR_GIVEUP_PROBABILITY}.
	 */
	private final double fGiveupProbability;

	/**
	 * Maximum size for the hot rumor list. If the list overgrows this, the
	 * "coldest" rumors start being evicted.
	 */
	private final int fMaxSize;
	
	/**
	 * Made a public field for efficiency reasons (this is accessed so frequently that
	 * encapsulating it makes simulation 10% slower).
	 */
	public int size;

	/** Random number generator. */
	private final Random fRandom;

	private final Linkable fConstraint;

	// ----------------------------------------------------------------------

	public RumorList(int maxSize, double giveupProbability,
			Linkable constraint, Random rnd) {
		fMaxSize = maxSize;
		fGiveupProbability = giveupProbability;
		fRandom = rnd;
		fConstraint = constraint;
	}

	// ----------------------------------------------------------------------

	public boolean add(IGossipMessage evt) {
		if (!addDestinations(evt)) {
			return false;
		}

		// Hottest rumors are at the END of the list.
		fHotRumors.addLast(evt.cloneIfNeeded());
		size++;
		if (fMaxSize > 0 && size > fMaxSize) {
			IGossipMessage discarded = fHotRumors.removeFirst();
			removeDestinations(discarded);
			size--;
		}

		return true;
	}

	// ----------------------------------------------------------------------

	public List<IGossipMessage> getList() {
		return fRoHotRumors;
	}

	// ----------------------------------------------------------------------

	public void dropAll(Node source) {
		Iterator<IGossipMessage> it = fHotRumors.iterator();
		while (it.hasNext()) {
			IGossipMessage msg = it.next();
			it.remove();
			drop(source, msg);
		}
	}

	// ----------------------------------------------------------------------

	private void drop(Node node, IGossipMessage discarded) {
		size--;
		discarded.dropped(node);
		removeDestinations(discarded);
	}

	// ----------------------------------------------------------------------

	public void demote(ArrayList<Boolean> mask, int size, Node node) {
		ListIterator<IGossipMessage> it = fHotRumors.listIterator(start(size));

		for (int i = 0; it.hasNext() && i < size; i++) {
			// Rumor didn't help.
			if (!mask.get(i)) {
				// Either discards ...
				if (fRandom.nextDouble() < fGiveupProbability) {
					IGossipMessage discarded = it.next();
					it.remove();
					drop(node, discarded);
				}
				// .. or demotes the Tweet.
				else if (it.hasPrevious()) {
					/**
					 * This piece of code simply pushes an element one position
					 * back into the list. The painful dance with the iterators
					 * is to be efficient with the linked list implementation,
					 * and avoid O(n) access.
					 */
					IGossipMessage evt = it.next();
					it.previous();
					IGossipMessage previous = it.previous();
					it.set(evt);
					it.next();
					it.next();
					it.set(previous);
				} else {
					it.next();
				}
			} else {
				it.next();
			}
		}
	}

	// ----------------------------------------------------------------------

	private int start(int size) {
		return Math.max(0, fHotRumors.size() - size);
	}

	// ----------------------------------------------------------------------

	protected abstract boolean addDestinations(IGossipMessage message);
	
	// ----------------------------------------------------------------------

	protected abstract void removeDestinations(IGossipMessage message);

	// ----------------------------------------------------------------------

	protected abstract int messagesFor(Node node);
	
	// ----------------------------------------------------------------------

	protected Linkable constraintLinkable() {
		return fConstraint;
	}
}
