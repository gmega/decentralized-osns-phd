package it.unitn.disi.newscasting.internal;

import it.unitn.disi.newscasting.Tweet;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import peersim.core.Node;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * Non-scalable, simple event storage which simply stores all {@link Tweet}
 * objects in-memory.
 * 
 * @author giuliano
 */
public class SimpleEventStorage implements IWritableEventStorage {
	
	private Multimap<Node, Tweet> fStore = HashMultimap.create();

	@Override
	public boolean add(Tweet tweet) {
		return fStore.put(tweet.poster, tweet);
	}

	@Override
	public boolean contains(Tweet tweet) {
		return fStore.containsEntry(tweet.poster, tweet);
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
	public Iterator<Tweet> tweetsFor(Node node) {
		return Collections.unmodifiableCollection(fStore.get(node)).iterator();
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
