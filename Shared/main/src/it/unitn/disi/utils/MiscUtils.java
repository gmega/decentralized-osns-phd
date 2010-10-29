package it.unitn.disi.utils;

import it.unitn.disi.utils.collections.IExchanger;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;


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
	
	public static void grow(ArrayList<?> list, int size) {
		while (list.size() < size) {
			list.add(null);
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
	
	
	public static double log2(double input) {
		return Math.log(input)/Math.log(2.0);
	}
}
