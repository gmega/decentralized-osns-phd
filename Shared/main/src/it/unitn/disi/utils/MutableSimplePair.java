package it.unitn.disi.utils;

import java.util.HashMap;

/**
 * A mutable pair. Do not use this as a key into a {@link HashMap}; for that use
 * {@link Pair}.
 * 
 * @author giuliano
 * 
 * @param <K>
 * @param <V>
 */
public class MutableSimplePair<K, V> {
	public K a;
	public V b;
	
	public MutableSimplePair(K a, V b) {
		this.a = a;
		this.b = b;
	}
	
	public String toString() {
		return "(" + this.a + ", " + this.b + ")";
	}
}
