package it.unitn.disi.utils;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MultiCounter<K> implements Comparator<K>, Iterable<K>, Cloneable{
	private HashMap<K, Integer> fHistory;
	
	private Map<K, Integer> fROStory;

	public MultiCounter () {
		fHistory = new HashMap<K, Integer>();
		fROStory = Collections.unmodifiableMap(fHistory);
	}
	
	public int hist(K id) {
		if (!fHistory.containsKey(id)) {
			return 0;
		}
			
		return fHistory.get(id);
	}
	
	public void increment(K id) {
		this.increment(id, 1);
	}
	
	public void increment(K id, int increment) {
		fHistory.put(id, hist(id) + increment);	
	}
	
	public int compare(K o1, K o2) {
		return hist(o1) - hist(o2);
	}

	public Iterator<K> iterator() {
		return fROStory.keySet().iterator();
	}
	
	public Map<K, Integer> asMap() {
		return fROStory;
	}
	
	public MultiCounter<K> clone() {
		try {
			@SuppressWarnings("unchecked")
			MultiCounter<K> clone = (MultiCounter<K>)super.clone();
			clone.fHistory = new HashMap<K, Integer>(fHistory);
			clone.fROStory = Collections.unmodifiableMap(clone.fHistory);
			return clone;
		} catch (CloneNotSupportedException ex) {
			throw new RuntimeException(ex);
		}
	}
}
