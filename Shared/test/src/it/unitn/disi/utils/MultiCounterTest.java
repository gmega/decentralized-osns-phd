package it.unitn.disi.utils;

import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import junit.framework.Assert;

public class MultiCounterTest {

	private Integer[] elements;
	private int[] counts;
	
	@Before
	public void generateElementsAndCounts() {
		int SIZE = 5000;
		Random rnd = new Random();

		elements = new Integer[SIZE];
		counts = new int[SIZE];
		for (int i = 0; i < elements.length; i++) {
			elements[i] = i;
			counts[i] = Math.abs(rnd.nextInt());
		}

		OrderingUtils.permute(0, 5000, elements, rnd);
	}
	
	@Test
	public void testSparseCounter() {
		runCounterTest(new SparseMultiCounter<Integer>());
	}
	
	@Test
	public void testDenseCounter() {
		runCounterTest(new DenseMultiCounter<Integer>(elements, new IKey<Integer>() {
			@Override
			public int key(Integer element) {
				return element;
			}
		}));
	}

	public void runCounterTest(IMultiCounter<Integer> counter) {
		for (int i = 0; i < elements.length; i++) {
			counter.increment(elements[i], counts[i]);
		}
		
		for (int i = 0; i < elements.length; i++) {
			Assert.assertEquals(counts[i], counter.count(elements[i]));
		}
		
		Assert.assertEquals(0, counter.count(5000));
	}
}
