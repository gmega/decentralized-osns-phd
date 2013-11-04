package it.unitn.disi.graph;

import it.unitn.disi.graph.BFSIterable.BFSIterator;
import it.unitn.disi.graph.codecs.ByteGraphDecoder;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;
import it.unitn.disi.graph.utils.TestUtils;
import it.unitn.disi.utils.collections.Pair;

import java.io.ByteArrayInputStream;
import java.util.NoSuchElementException;

import junit.framework.Assert;

import org.junit.Test;

public class BFSIterableTest {
	@Test
	public void testBFS() throws Exception {
		String graphStr = " 0 1 2 3 4\n" +
				"1 2\n" +
				"2 3\n" +
				"3 4\n" +
				"4 1 7\n" + 
				"5 6 7 8\n" +
				"6 7\n" +
				"7 8\n" +
				"8 9\n" +
				"9 6\n";
		
		@SuppressWarnings("unchecked")
		Pair<Integer, Integer> [][] expected = new Pair [][] {
				{ p(0,0), p(1,1), p(2,1), p(3,1), p(4,1), p(7,2) },
				{},
				{},
				{},
				{ p(4,0), p(0,1), p(1,1), p(3,1), p(7,1), p(2,2), p(5,2), p(6,2), p(8,2) }
		};
		
		ByteArrayInputStream encoded = TestUtils.undirect(TestUtils.encode(graphStr));
		IndexedNeighborGraph graph = LightweightStaticGraph.load(new ByteGraphDecoder(encoded));
		
		for (int i = 0; i < expected.length; i++) {
			Pair<Integer, Integer> [] search = expected[i];
			int searchLength = search.length;
			BFSIterator it = new BFSIterator(graph, i);
			for (int j = 0; j < searchLength; j++) {
				Pair <Integer, Integer> next = it.next();
				assertContains(next, search);
			}
			
			while (it.hasNext()) {
				it.next();
				searchLength++;
			}
			
			Assert.assertTrue(searchLength == graph.size());
			
			try {
				it.next();
				Assert.fail();
			} catch (NoSuchElementException ex) { }
		}
	}
	
	public static <T> void assertContains(T element, T [] array) {
		for (int i = 0; i < array.length; i++) {
			if (element.equals(array[i])) {
				return;
			}
		}
		
		Assert.fail("Element " + element.toString() + " not found.");
	}

	
	private Pair<Integer, Integer> p(int node, int depth) {
		return new Pair<Integer,Integer>(node, depth);
	}
}
