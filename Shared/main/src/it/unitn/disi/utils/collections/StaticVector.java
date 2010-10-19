package it.unitn.disi.utils.collections;

import it.unitn.disi.utils.MiscUtils;

import java.util.Arrays;
import java.util.Collections;

/**
 * Lightweight, static vector, conceived with maximum efficiency in mind.
 * 
 * @author giuliano
 */
public class StaticVector<T extends Object> {

	private T [] fData;
	
	private int fSize;
	
	public StaticVector() {
		fData = alloc(4);
	}
	
	public void resize(int size, boolean copy) {
		if (size < fData.length) {
			return;
		}
		
		int powerOfTwo = (int) Math.round(MiscUtils.log2(size));
		T[] resized = alloc((int) Math.round(Math.pow(2, powerOfTwo + 1)));
		if (copy) {
			System.arraycopy(fData, 0, resized, 0, fSize);
		} else {
			fSize = 0;
		}
		
		fData = resized;
	}
	
	public boolean contains(T object) {
		return MiscUtils.contains(fData, object);
	}
	
	public T get(int i) {
		return (T) fData[i];
	}
	
	public void append(T object) {
		fData[fSize++] = object;
	}
	
	public int size() {
		return fSize;
	}
	
	public void clear() {
		fSize = 0;
	}
	
	public void deepClear() {
		clear();
		Arrays.fill(fData, null);
	}

	@SuppressWarnings("unchecked")
	private T[] alloc(int size) {
		return (T[]) new Object[size];
	}
}
