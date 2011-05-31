package it.unitn.disi.utils.collections;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Simple {@link LinkedHashMap} extension which provides a bounded-size
 * {@link Map}, implementing an LRU eviction policy.
 * 
 * @author giuliano
 */
public class BoundedHashMap<K, V> extends LinkedHashMap<K, V> {

	private static final long serialVersionUID = -1322650980871833784L;

	private final int fMax;

	private long fDiscarded;

	public BoundedHashMap(int max) {
		super();
		fMax = max;
	}

	/**
	 * @return the maximum size this {@link BoundedHashMap} can grow to.
	 */
	public int maxSize() {
		return fMax;
	}

	/**
	 * @return how many entries have been discarded by the LRU policy.
	 */
	public long discarded() {
		return fDiscarded;
	}

	@Override
	protected boolean removeEldestEntry(Entry<K, V> eldest) {
		if (size() == fMax) {
			fDiscarded++;
			return true;
		}

		return false;
	}
}
