package it.unitn.disi.utils;

import java.util.Arrays;
import java.util.BitSet;
import java.util.NoSuchElementException;
import java.util.Random;

import junit.framework.Assert;

import org.junit.Test;

public class IDMapperTest {

	@Test
	public void sparseSimpleOps() {
		AbstractIDMapper mapper = new SparseIDMapper();
		simpleOps(mapper);
		mapper.clear();
		simpleOps(mapper);
	}

	@Test
	public void denseSimpleOps() {
		AbstractIDMapper mapper = new DenseIDMapper();
		simpleOps(mapper);
		mapper.clear();
		simpleOps(mapper);
	}
	
	@Test
	public void sparseRandomVector() {
		mapRandomVector(new SparseIDMapper());
	}
	
	@Test
	public void denseRandomVector() {
		mapRandomVector(new DenseIDMapper());
	}

	public void simpleOps(AbstractIDMapper mapper) {
		Assert.assertEquals(0, mapper.addMapping(1));
		Assert.assertEquals(1, mapper.addMapping(3));
		Assert.assertEquals(2, mapper.addMapping(5));

		Assert.assertEquals(0, mapper.addMapping(1));
		Assert.assertEquals(1, mapper.addMapping(3));
		Assert.assertEquals(2, mapper.addMapping(5));

		Assert.assertFalse(mapper.isMapped(0));
		Assert.assertTrue(mapper.isMapped(1));
		Assert.assertFalse(mapper.isMapped(2));
		Assert.assertTrue(mapper.isMapped(3));
		Assert.assertFalse(mapper.isMapped(4));
		Assert.assertTrue(mapper.isMapped(5));
		Assert.assertFalse(mapper.isMapped(6));
		Assert.assertFalse(mapper.isMapped(Integer.MAX_VALUE));

		try {
			mapper.map(0);
			Assert.fail();
		} catch (NoSuchElementException ex) {
		}

		Assert.assertEquals(1, mapper.reverseMap(0));
		Assert.assertEquals(3, mapper.reverseMap(1));
		Assert.assertEquals(5, mapper.reverseMap(2));
	}

	public void mapRandomVector(AbstractIDMapper mapper) {
		int[] rndVector = new int[1000];
		Random rnd = new Random();
		BitSet drawn = new BitSet();
		for (int i = 0; i < rndVector.length; i++) {
			int candidate = rnd.nextInt(2000000);
			if (drawn.get(candidate)) {
				i--;
				continue;
			}
			drawn.set(candidate);
			rndVector[i] = candidate;
			Assert.assertEquals(i, mapper.addMapping(rndVector[i]));
		}
		
		Assert.assertEquals(mapper.size(), rndVector.length);
		
		for (int i = 0; i < rndVector.length; i++) {
			Assert.assertEquals(i, mapper.map(rndVector[i]));
			Assert.assertEquals(rndVector[i], mapper.reverseMap(i));
		}
		
		Arrays.sort(rndVector);
		int k = 0;
		for (int i = 0; i < 2000000 && k < rndVector.length; i++) {
			if (i == rndVector[k]) {
				Assert.assertTrue(mapper.isMapped(i));
				k++;
			} else {
				Assert.assertFalse(mapper.isMapped(i));
			}
		}
	}

}
