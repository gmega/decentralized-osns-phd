package it.unitn.disi.sps;

import it.unitn.disi.utils.graph.SubgraphDecorator;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;

import peersim.graph.BitMatrixGraph;
import peersim.graph.Graph;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class TestGraphWrapper{
	@Test public void graphWrapper() throws Exception {
		Graph g = new BitMatrixGraph(6);
		
		for (int i = 0; i < g.size(); i++) {
			g.setEdge(i, (i + 1) % 6);
		}
		
		g.setEdge(0, 3);
		g.setEdge(1, 4);
		
		assertListEquals(g.getNeighbours(0), 1, 3);
		assertListEquals(g.getNeighbours(1), 2, 4);
		assertListEquals(g.getNeighbours(2), 3);
		
		SubgraphDecorator sbg = new SubgraphDecorator(g, true);
		ArrayList<Integer> lst = new ArrayList<Integer>();
		lst.add(0);
		lst.add(1);
		lst.add(2);
		
		sbg.setVertexList(lst);
		
		assertListEquals(sbg.getNeighbours(0), 1);
		assertListEquals(sbg.getNeighbours(1), 2);
		assertListEquals(sbg.getNeighbours(2));	
	}

	private void assertListEquals(Collection<Integer> neighbours, int...i) {
		assertEquals(neighbours.size(), i.length);
		
		for (int id : i) {
			assertTrue(neighbours.contains(id));
		}
	}
}
