package it.unitn.disi.graph;

import it.unitn.disi.graph.lightweight.LightweightStaticGraph;
import it.unitn.disi.graph.lightweight.LightweightStaticGraphEID;

import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import peersim.graph.BitMatrixGraph;
import peersim.graph.GraphFactory;

public class LightweightStaticGraphEIDTest {
	
	final int OUT_DEGREE = 10;

	@Test
	public void testUndirectedEdgeIds() {
		BitMatrixGraph bmg = new BitMatrixGraph(10000, false);
		GraphFactory.wireKOut(bmg, OUT_DEGREE, new Random(42));

		LightweightStaticGraph lsg = LightweightStaticGraph.fromGraph(bmg);
		LightweightStaticGraphEID eid = LightweightStaticGraphEID
				.asLightweightStaticGraphEID(lsg);

		int k = 0;
		for (int i = 0; i < lsg.size(); i++) {
			int degree = lsg.degree(i);
			for (int j = 0; j < degree; j++) {
				int neighbor = lsg.getNeighbor(i, j);
				int id = eid.edgeId(i, neighbor);

				// Index ID should be the same.
				Assert.assertEquals(id, eid.edgeId(i, neighbor, j));

				// Undirected edges should have the same id.
				if (neighbor < i) {
					Assert.assertEquals(id, eid.edgeId(neighbor, i));
					// Should also work with the index.
					int idx = lsg.indexOf(neighbor, i);
					Assert.assertEquals(id, eid.edgeId(neighbor, i, idx));
				} else {
					Assert.assertEquals(id, k);
					k++;
				}

			}
		}
	}

}
