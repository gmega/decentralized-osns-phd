package it.unitn.disi.utils;

import java.util.Arrays;
import java.util.NoSuchElementException;

/**
 * {@link DenseMultiCounter} is a memory-efficient alternative to
 * {@link SparseMultiCounter} when the number of elements becomes too large.
 * Memory efficiency comes at the cost of complexity and slower operation.
 * 
 * @author giuliano
 * 
 * @param <K>
 */
public class DenseMultiCounter<K> implements IMultiCounter<K> {

	private final int [] fElements;

	private final int [] fCounters; 
	
	private final IKey<K> fKey;

	public DenseMultiCounter(K[] elements, IKey<K> key) {
		fKey = key;
		fElements = new int[elements.length];
		fCounters = new int[elements.length];
		for (int i = 0; i < elements.length; i++) {
			fElements[i] = key.key(elements[i]);
		}
		Arrays.sort(fElements);
	}

	@Override
	public int compare(K o1, K o2) {
		return count(o1) - count(o2);
	}

	@Override
	public void increment(K element) {
		increment(element, 1);
	}

	@Override
	public void decrement(K element) {
		decrement(element, 1);
	}

	@Override
	public void increment(K element, int increment) {
		int idx = search(element, false);
		fCounters[idx] += increment;
	}

	@Override
	public void decrement(K element, int decrement) {
		int idx = search(element, true);
		if (idx == -1) {
			return;
		}
		fCounters[idx] = Math.max(0, fCounters[idx] - decrement);
	}

	@Override
	public int count(K element) {
		int idx = search(element, true);
		if (idx == -1) {
			return 0;
		}
		return fCounters[idx];
	}

	private int search(K element, boolean canFail) {
		int key = fKey.key(element);
		int index = Arrays.binarySearch(fElements, key);
		if (index >= 0 && index < fElements.length && fElements[index] == key) {
			return index;
		}

		// Not found.
		if (canFail) {
			return -1;
		}

		throw new NoSuchElementException("Unknown element: " + element);
	}

}
