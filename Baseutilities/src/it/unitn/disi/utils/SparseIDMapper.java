package it.unitn.disi.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SparseIDMapper extends AbstractIDMapper {

	private final Map<Integer, Integer> fMap = new HashMap<Integer, Integer>();
	
	private ArrayList<Integer> fReverseMap = new ArrayList<Integer>();

	@Override
	protected int get(int id) {
		Integer mapped = fMap.get(id);
		if (mapped == null) {
			return UNMAPPED;
		}
		return mapped;
	}

	@Override
	protected int reverseGet(int id) {
		if (id >= fReverseMap.size() || id < 0) {
			return UNMAPPED;
		}
		return fReverseMap.get(id);
	}

	@Override
	protected void addMapping(int id, int value) {
		if (isMapped(id)) {
			return;
		}
		
		fMap.put(id, value);
		
		fReverseMap.ensureCapacity(value);
		for (int i = fReverseMap.size(); i < value; i++) {
			fReverseMap.add(UNMAPPED);
		}
		
		fReverseMap.add(value, id);
	}
	
	@Override
	public void clear() {
		super.clear();
		fMap.clear();
		fReverseMap.clear();
	}

}
