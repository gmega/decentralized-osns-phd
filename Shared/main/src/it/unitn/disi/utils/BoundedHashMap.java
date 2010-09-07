package it.unitn.disi.utils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Simple {@link LinkedHashMap} extension which provides a bounded-size
 * {@link Map}, implementing an LRU eviction policy.
 * 
 * @author giuliano
 */
public class BoundedHashMap <K, V> extends LinkedHashMap<K, V>{

	private static final long serialVersionUID = -1322650980871833784L;

	private final int fMax;
	
	public BoundedHashMap (int max) {
		super();
		fMax = max;
	}

	@Override
	protected boolean removeEldestEntry(Entry<K, V> eldest) {
		if (size() == fMax) {
			return true;
		}

		return false;	
	}
}
