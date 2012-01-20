package it.unitn.disi.utils;

import java.util.Arrays;

public class DenseIDMapper extends AbstractIDMapper {

	private int[] fMap;

	private int[] fReverseMap;

	public DenseIDMapper() {
		this(10);
	}
	
	public DenseIDMapper(int size) {
		super();
		clear();
	}
	
	public int [] mappings() {
		int [] map = new int[fMap.length];
		System.arraycopy(fMap, 0, map, 0, map.length);
		return map;
	}
	
	public int [] reverseMappings() {
		int [] revMap = new int[fReverseMap.length];
		System.arraycopy(fReverseMap, 0, revMap, 0, revMap.length);
		return revMap;
	}

	@Override
	protected int get(int id) {
		return arrayGet(fMap, id);
	}

	@Override
	protected int reverseGet(int id) {
		return arrayGet(fReverseMap, id);
	}
	
	private int arrayGet(int [] array, int id) {
		if (id >= array.length) {
			return UNMAPPED;
		}
		return array[id];
	}
	
	@Override
	protected void addMapping(int id, int value) {
		fMap = ensure(fMap, id);
		fReverseMap = ensure(fReverseMap, value);

		fMap[id] = value;
		fReverseMap[value] = id;
	}
	
	public void clear() {
		super.clear();
		fMap = new int[10];
		fReverseMap = new int[10];
		Arrays.fill(fMap, UNMAPPED);
		Arrays.fill(fReverseMap, UNMAPPED);
	}

	private int[] ensure(int[] array, int id) {
		int[] resized = array;
		if (array.length <= (id + 1)) {
			int powerOfTwo = (int) Math.round(MiscUtils.log2(Math.max(array.length, id)));
			resized = new int[(int) Math.round(Math.pow(2, powerOfTwo + 1))];
			System.arraycopy(array, 0, resized, 0, array.length);
			Arrays.fill(resized, array.length, resized.length, UNMAPPED);
		}
		return resized;
	}
}
