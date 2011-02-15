package it.unitn.disi.utils;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Simple class which helps with id remapping operations.
 * 
 * @author giuliano
 */
public class IdMapper {
	
	private final HashMap<Integer, Integer> fMap;
	
	private final ArrayList<Integer> fInverse;
	
	public IdMapper() {
		fMap = new HashMap<Integer, Integer>();
		fInverse = new ArrayList<Integer>();
	}
	
	public int map(int id) {
		Integer mapped = fMap.get(id);
		if (mapped == null) {
			mapped = fInverse.size();
			fMap.put(id, mapped);
			fInverse.add(id);
		}
		return mapped;
	}
	
	public int reverseMap(int id) {
		if (id >= fInverse.size()) {
			throw new IllegalArgumentException();
		}
		return fInverse.get(id);
	}

	public boolean isMapped(int id) {
		return fMap.containsKey(id);
	}
	
	public int size() {
		return fMap.size();
	}
}
