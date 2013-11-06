package it.unitn.disi.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * {@link SparseMultiCounter} allows clients to count instances of objects of a
 * given type. It's memory efficient for sparse data.
 * 
 * @author giuliano
 * 
 * @param <K>
 */
public class SparseMultiCounter<K> implements Iterable<K>, Cloneable,
		IMultiCounter<K> {

	private HashMap<K, Integer> fHistory;

	private Map<K, Integer> fROStory;

	public SparseMultiCounter() {
		fHistory = new HashMap<K, Integer>();
		fROStory = Collections.unmodifiableMap(fHistory);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.unitn.disi.utils.IMultiCounter#count(K)
	 */
	@Override
	public int count(K id) {
		if (!fHistory.containsKey(id)) {
			return 0;
		}

		return fHistory.get(id);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.unitn.disi.utils.IMultiCounter#increment(K)
	 */
	@Override
	public void increment(K id) {
		this.increment(id, 1);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.unitn.disi.utils.IMultiCounter#decrement(K)
	 */
	@Override
	public void decrement(K id) {
		this.decrement(id, 1);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.unitn.disi.utils.IMultiCounter#increment(K, int)
	 */
	@Override
	public void increment(K id, int increment) {
		if (increment < 0) {
			throw new IllegalArgumentException("Increments must be positive.");
		}
		fHistory.put(id, count(id) + increment);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.unitn.disi.utils.IMultiCounter#decrement(K, int)
	 */
	@Override
	public void decrement(K id, int decrement) {
		int count = count(id);
		if (count == 0) {
			return;
		}

		count -= decrement;
		if (count <= 0) {
			fHistory.remove(id);
		} else {
			fHistory.put(id, count);
		}
	}
	
	public int size() {
		return fHistory.size();
	}
	
	public void clear() {
		fHistory.clear();
	}

	public int compare(K o1, K o2) {
		return count(o1) - count(o2);
	}

	public Iterator<K> iterator() {
		return fROStory.keySet().iterator();
	}

	public Map<K, Integer> asMap() {
		return fROStory;
	}

	public SparseMultiCounter<K> clone() {
		try {
			@SuppressWarnings("unchecked")
			SparseMultiCounter<K> clone = (SparseMultiCounter<K>) super.clone();
			clone.fHistory = new HashMap<K, Integer>(fHistory);
			clone.fROStory = Collections.unmodifiableMap(clone.fHistory);
			return clone;
		} catch (CloneNotSupportedException ex) {
			throw new RuntimeException(ex);
		}
	}
}
