package it.unitn.disi.graph;

import it.unitn.disi.graph.generators.CompleteGraph;

import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Test;

public class CompleteGraphTest {
	@Test
	public void testCompleteGraph() throws Exception {
		
		CompleteGraph cg = new CompleteGraph(100);
		
		Assert.assertEquals(100, cg.size());
		
		for (int i = 0; i < 100; i++) {
			Assert.assertEquals(99, cg.degree(i));
			
			Set<Integer> all = new HashSet<Integer>();
			for (int j = 0; j < 100; j++) {
				if (i != j) {
					all.add(j);
				}
			}
			
			for (int j = 0; j < cg.degree(i); j++) {
				Assert.assertTrue(all.remove(cg.getNeighbor(i, j)));
			}
			
			Assert.assertTrue(all.isEmpty());
		}
		
	}
}
