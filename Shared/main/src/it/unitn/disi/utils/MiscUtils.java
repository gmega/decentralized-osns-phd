package it.unitn.disi.utils;

import it.unitn.disi.utils.collections.IExchanger;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;


/**
 * Disarrayed collection of functions.
 * 
 * @author giuliano
 *
 */
public class MiscUtils {
	
	public static void safeClose(Closeable is, boolean rethrow) {
		try {
			if (is != null) {
				is.close();
			}
		} catch (IOException ex) {
			if (rethrow) {
				throw new RuntimeException(ex);
			}
			ex.printStackTrace();
		}
	}
	
	public static RuntimeException nestRuntimeException(Exception ex) {
		if (ex instanceof RuntimeException) {
			return (RuntimeException) ex;
		} else {
			return new RuntimeException(ex);
		}
	}
	
	public static <T> void grow(ArrayList<T> list, int size, T fill) {
		while (list.size() < size) {
			list.add(fill);
		}
	}
	
	public static int compact(Object[] array, IExchanger exch, int end) {
		int gap = 0;
		
		for (int i = 0; i < end; i++) {
			if (array[i] == null) {
				gap++;
				continue;
			}
			if (gap == 0) {
				continue;
			}
			
			exch.exchange(i - gap, i);
		}
		
		return end - gap;
	}
	
	
	public static int compact(Object[] array, int end) {
		int gap = 0;
		
		for (int i = 0; i < end; i++) {
			if (array[i] == null) {
				gap++;
				continue;
			}
			if (gap == 0) {
				continue;
			}
			
			array[i - gap] = array[i];
			array[i] = null;
		}
		
		return end - gap;
	}
	
	public static <T> boolean contains (T [] array, T object) { 
		for (int i = 0; i < array.length; i++) {
			if (object.equals(array[i])) {
				return true;
			}
		}
		return false;
	}
	
	public static <T> int lastDifferentFrom(T [] array, T element) {
		int i = array.length - 1;
		boolean equals = true;
		for (; i >= 0; i--) {
			if (array[i] == null) {
				equals = (element == null);
			} else {
				equals = array[i].equals(element);
			}
			
			if (!equals) {
				break;
			}
		}
		return i;
	}
	
	public static double log2(double input) {
		return Math.log(input)/Math.log(2.0);
	}
	
	public static int integers(int count) {
		int size = Integer.SIZE / Byte.SIZE;
		return size * count;
	}
	
	public static int pointers(int count) {
		int size = Integer.parseInt(System.getProperty("sun.arch.data.model"))/Byte.SIZE;
		return size*count;
	}
}
