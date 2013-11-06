package it.unitn.disi.graph.analysis;

import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.algorithms.DunnTopK;
import it.unitn.disi.graph.algorithms.DunnTopK.Mode;
import it.unitn.disi.graph.algorithms.PathEntry;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;

import java.util.ArrayList;
import java.util.Arrays;

import junit.framework.Assert;

import org.junit.Test;

public class TestTopKShortestDisjoint {

	@Test
	public void testVertexDisjoint() {
		LightweightStaticGraph lsg = LightweightStaticGraph
			.fromAdjacency(new int[][] { 
				{ 1, 4, 6 }, 
				{ 0, 2, 4, 5 },
				{ 1, 3, 6, 7 }, 
				{ 2, 5, 7 },
				{ 0, 1 },
				{ 1, 3 },
				{ 0, 2 },
				{ 2, 3 }
			});
		
		double [][] weights = new double[8][8];
		for (int i = 0; i < weights.length; i++) {
			Arrays.fill(weights[i], 1.0);
		}
		
		int [][] refPaths = {
				{ 0, 1, 2, 3 },
				{ 0, 4, 1, 5, 3},
				{ 0, 6, 2, 7, 3}
		};
		
		double[] costs = { 3.0, 4.0, 4.0 };
		
		runTest(lsg, weights, costs, Mode.EdgeDisjoint, refPaths, 0, 3, 10);
	}

	@Test
	public void testEdgeDisjoint() {
		LightweightStaticGraph lsg = LightweightStaticGraph
				.fromAdjacency(new int[][] { 
						{ 1, 2, 3 }, 
						{ 0, 2, 3 },
						{ 0, 1, 3 }, 
						{ 0, 1, 2 }
					});

		double[][] weights = { 
				{ 1.0, 1.0, 1.0, 1.0 },
				{ 1.0, 1.0, 1.0, 1.0 }, 
				{ 1.0, 1.0, 1.0, 1.0 },
				{ 1.0, 1.0, 1.0, 1.0 } 
			};
		
		int [][] refPaths = new int[][] {
				{ 0, 2 },
				{ 0, 1, 2 },
				{ 0, 3, 2 }
		};
		
		double[] costs = new double[] { 1.0, 2.0, 2.0 };

		runTest(lsg, weights, costs, Mode.VertexDisjoint, refPaths, 0, 2, 10);
	}

	public void runTest(IndexedNeighborGraph lsg, double[][] weights,
			double[] costs, Mode mode, int[][] refPaths, int source,
			int destination, int k) {

		DunnTopK tsd = new DunnTopK(lsg, weights,
				Mode.EdgeDisjoint);

		ArrayList<PathEntry> paths = tsd.topKShortest(source, destination, k);
		Assert.assertEquals(refPaths.length, paths.size());

		for (int i = 0; i < paths.size(); i++) {
			PathEntry actual = paths.get(i);
			Assert.assertEquals(costs[i], actual.cost);
			

			for (int j = 0; j < refPaths[i].length; j++) {
				Assert.assertEquals(refPaths[i].length, actual.path.length);
				Assert.assertEquals(refPaths[i][j], actual.path[j]);
			}
		}

	}
}
