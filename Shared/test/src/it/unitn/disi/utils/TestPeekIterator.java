package it.unitn.disi.utils;

import java.util.ArrayList;
import java.util.NoSuchElementException;

import org.junit.Assert;
import org.junit.Test;

public class TestPeekIterator {
	@Test
	public void peekIteratorTest() {
		ArrayList<Integer> list = new ArrayList<Integer>();
		
		for (int i = 0; i < 10;  i++) {
			list.add(i);
		}
		
		PeekIterator<Integer> peek = new PeekIterator<Integer>(list.iterator());
		
		for (int i = 0; i < 10; i++) {
			Assert.assertTrue(peek.hasNext());
			for (int j = 0; j < 10; j++) {
				Assert.assertEquals(i, peek.peek().intValue());
			}
			Assert.assertEquals(i, peek.next().intValue());			
		}
		
		Assert.assertFalse(peek.hasNext());
		
		try {
			peek.next();
			Assert.fail();
		} catch (NoSuchElementException ex) {
			
		}
	}
}
