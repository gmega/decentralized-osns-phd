package it.unitn.disi.newscasting.internal;

import it.unitn.disi.epidemics.IGossipMessage;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import peersim.core.Node;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * Non-scalable, simple event storage which simply stores all
 * {@link IGossipMessage} objects in-memory.
 * 
 * @author giuliano
 */
public class SimpleEventStorage implements IWritableEventStorage {

	private Multimap<Node, IGossipMessage> fStore = HashMultimap.create();

	@Override
	public boolean add(IGossipMessage m) {
		return fStore.put(m.originator(), m);
	}

	@Override
	public boolean contains(IGossipMessage m) {
		return fStore.containsEntry(m.originator(), m);
	}

	@Override
	public int distinctNodes() {
		return fStore.keySet().size();
	}

	@Override
	public int elements() {
		return fStore.size();
	}

	@Override
	public Set<Node> nodes() {
		return Collections.unmodifiableSet(fStore.keySet());
	}

	@Override
	public Iterator<IGossipMessage> tweetsFor(Node node) {
		return (Iterator<IGossipMessage>) Collections.unmodifiableCollection(
				fStore.get(node)).iterator();
	}

	@Override
	public void clear() {
		fStore.clear();
	}

	@Override
	public Object clone() {
		try {
			SimpleEventStorage clone = (SimpleEventStorage) super.clone();
			clone.fStore = HashMultimap.create(this.fStore);
			return clone;
		} catch (CloneNotSupportedException ex) {
			throw new RuntimeException(ex);
		}
	}
}
