package it.unitn.disi.utils.collections;

import it.unitn.disi.utils.MiscUtils;

import java.util.Arrays;

/**
 * Lightweight, static vector, conceived with maximum efficiency in mind.
 * 
 * @author giuliano
 */
public class StaticVector<T extends Object> {

	/**
	 * Public for efficiency reasons, should be treated as read-only.
	 */
	public T[] data;
	
	private int fSize;
	
	private int fPow = 0;

	public StaticVector() {
		data = alloc(0);
	}
	
	public void resize(int size, boolean copy) {
		if (size < data.length) {
			return;
		}
		
		int powerOfTwo = (int) Math.round(MiscUtils.log2(size));
		
		data = resized;
	}
	
	public void append(T object) {
		data[fSize++] = object;
	}
	
	public int size() {
		return fSize;
	}
	
	public void clear() {
		fSize = 0;
	}
	
	public void deepClear() {
		clear();
		Arrays.fill(data, null);
	}

	@SuppressWarnings("unchecked")
	private T[] alloc(int size) {
		return (T[]) new Object[size];
	}
}
