package it.unitn.disi.newscasting.internal;

import it.unitn.disi.epidemics.IGossipMessage;
import it.unitn.disi.epidemics.IWritableEventStorage;
import it.unitn.disi.utils.collections.BoundedHashMap;
import it.unitn.disi.utils.collections.Pair;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import peersim.core.Node;

/**
 * Non-scalable, simple event storage which simply stores all
 * {@link IGossipMessage} objects in-memory. Supports windowing.<BR>
 * <BR>
 * Neither {@link #nodes()} nor {@link #tweetsFor(Node)} are very efficient
 * under this implementation. If you need those often, better go for something
 * else.
 * 
 * @author giuliano
 */
public class SimpleEventStorage implements IWritableEventStorage {

	/**
	 * Message storage.
	 */
	private BoundedHashMap<Pair<Node, Integer>, IGossipMessage> fStore;

	public SimpleEventStorage() {
		this(Integer.MAX_VALUE);
	}

	public SimpleEventStorage(int windowSize) {
		fStore = new BoundedHashMap<Pair<Node, Integer>, IGossipMessage>(
				windowSize);
	}

	@Override
	public boolean add(IGossipMessage m) {
		Pair<Node, Integer> key = new Pair<Node, Integer>(m.originator(),
				m.sequenceNumber());
		IGossipMessage current = fStore.get(key);
		if (current == null) {
			fStore.put(key, m);
			return true;
		}
		return false;
	}

	@Override
	public IGossipMessage retrieve(Node originator, int sequence) {
		return fStore.get(new Pair<Node, Integer>(originator, sequence));
	}

	@Override
	public boolean contains(IGossipMessage m) {
		return fStore.containsKey(new Pair<Node, Integer>(m.originator(), m
				.sequenceNumber()));
	}

	@Override
	public int elements() {
		return fStore.size();
	}

	@Override
	public int distinctNodes() {
		return nodes().size();
	}

	@Override
	public Set<Node> nodes() {
		Set<Node> nodes = new HashSet<Node>();
		for (Pair<Node, Integer> messageKey : fStore.keySet()) {
			nodes.add(messageKey.a);
		}
		return nodes;
	}

	@Override
	public Iterator<IGossipMessage> tweetsFor(Node node) {
		ArrayList<IGossipMessage> msgs = new ArrayList<IGossipMessage>();
		for (Pair<Node, Integer> messageKey : fStore.keySet()) {
			if (messageKey.a.equals(node)) {
				msgs.add(fStore.get(messageKey));
			}
		}
		return msgs.iterator();
	}

	@Override
	public void clear() {
		fStore.clear();
	}

	@Override
	public boolean remove(Node node, int sequence) {
		return fStore.remove(new Pair<Node, Integer>(node, sequence)) != null;
	}

	@Override
	public Object clone() {
		try {
			SimpleEventStorage clone = (SimpleEventStorage) super.clone();
			clone.fStore = new BoundedHashMap<Pair<Node, Integer>, IGossipMessage>(
					fStore.maxSize());
			return clone;
		} catch (CloneNotSupportedException ex) {
			throw new RuntimeException(ex);
		}
	}

	public long discarded() {
		return fStore.discarded();
	}
}
