package it.unitn.disi.newscasting.internal;

import it.unitn.disi.epidemics.IGossipMessage;
import it.unitn.disi.epidemics.IWritableEventStorage;
import it.unitn.disi.utils.collections.BoundedHashMap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
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
	 * Set where we store events. Using a {@link Map} looks bad but it's
	 * actually what {@link LinkedHashSet} does under the covers.
	 */
	private BoundedHashMap<IGossipMessage, IGossipMessage> fStore;

	public SimpleEventStorage() {
		this(Integer.MAX_VALUE);
	}

	public SimpleEventStorage(int windowSize) {
		fStore = new BoundedHashMap<IGossipMessage, IGossipMessage>(windowSize);
	}

	@Override
	public boolean add(IGossipMessage m) {
		IGossipMessage current = fStore.get(m);
		if (current == null) {
			fStore.put(m, m);
			return true;
		}
		return false;
	}
	
	@Override
	public IGossipMessage retrieve(IGossipMessage key) {
		return fStore.get(key);
	}

	@Override
	public boolean contains(IGossipMessage m) {
		return fStore.containsKey(m);
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
		for (IGossipMessage message : fStore.keySet()) {
			nodes.add(message.originator());
		}
		return nodes;
	}

	@Override
	public Iterator<IGossipMessage> tweetsFor(Node node) {
		ArrayList<IGossipMessage> msgs = new ArrayList<IGossipMessage>();
		for (IGossipMessage message : fStore.keySet()) {
			if (message.originator().equals(node)) {
				msgs.add(message);
			}
		}
		return msgs.iterator();
	}

	@Override
	public void clear() {
		fStore.clear();
	}

	@Override
	public boolean remove(IGossipMessage msg) {
		return fStore.remove(msg) != null;
	}

	@Override
	public Object clone() {
		try {
			SimpleEventStorage clone = (SimpleEventStorage) super.clone();
			clone.fStore = new BoundedHashMap<IGossipMessage, IGossipMessage>(
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
