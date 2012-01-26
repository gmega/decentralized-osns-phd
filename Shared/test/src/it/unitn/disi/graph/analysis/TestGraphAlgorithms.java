package it.unitn.disi.graph.analysis;

import junit.framework.Assert;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;

import org.junit.Test;

public class TestGraphAlgorithms {
	@Test
	public void testDijkstra() {
		IndexedNeighborGraph g = LightweightStaticGraph.fromAdjacency(new int[][]{
				{1, 2, 3, 4, 5},
				{0, 5},
				{0, 3},
				{0, 2, 4, 6},
				{0, 3, 5},
				{0, 1, 4},
				{3}
		});
		
		double [][] weights = new double[][] {
				{0,    64,   470,  550,  700,  540,  -1  },
				{64,   0,   -1,   -1,   -1,    536,  -1  },
				{470, -1,    0,    260, -1,   -1,    -1  },
				{550, -1,    260,  0,    150, -1,     250},
				{700, -1,   -1,    150,  0,    680,  -1  },
				{540,  536, -1,   -1,    680,  0,    -1  },
				{-1,  -1,   -1 ,   250, -1,   -1,     0  }
		};
		
		double [] minDists = new double[g.size()];
		int [] previous = new int[g.size()];
		
		GraphAlgorithms.dijkstra(g, 6, weights, minDists, previous);
		
		Assert.assertEquals(250.0, minDists[3]);
		Assert.assertEquals(800.0, minDists[0]);
		Assert.assertEquals(864.0, minDists[1]);
		
		GraphAlgorithms.dijkstra(g, 1, weights, minDists, previous);
		
		Assert.assertEquals(64.0, minDists[0]);
		Assert.assertEquals(614.0, minDists[3]);
		Assert.assertEquals(864.0, minDists[6]);
	}
}
