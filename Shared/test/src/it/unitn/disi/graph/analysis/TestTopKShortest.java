package it.unitn.disi.graph.analysis;

import java.util.ArrayList;

import junit.framework.Assert;

import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.analysis.TopKShortest.PathEntry;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;

import org.junit.Test;

public class TestTopKShortest {

	@Test
	public void testDAG() {
		IndexedNeighborGraph idg = LightweightStaticGraph.fromAdjacency(new int [][] {
				{1, 2, 4},
				{2, 4},
				{3, 4},
				{4},
				{}
		});
		
		double [][] weights = new double[][] {
				{0.0, 1.0, 1.0, 0.0, 9.0},
				{0.0, 0.0, 1.0, 0.0, 3.0},
				{0.0, 0.0, 0.0, 2.0, 1.0},
				{0.0, 0.0, 0.0, 0.0, 2.0},
				{0.0, 0.0, 0.0, 0.0, 0.0}
		};
		
		int [][] refPaths = new int[][] {
				{0, 2, 4},
				{0, 1, 2, 4},
				{0, 1, 4},
				{0, 2, 3, 4},
				{0, 1, 2, 3, 4},
				{0, 4}
		};
		
		double [] costs = {2, 3, 4, 5, 6, 9};
		
		runTest(idg, weights, refPaths, costs, 0, 4);
	}
	
	@Test
	public void testInstance_6_1() {
		IndexedNeighborGraph idg = LightweightStaticGraph.fromAdjacency(new int [][] {
				{1, 5},
				{2, 3, 5},
				{5},
				{5},
				{0, 1, 2, 3},
				{},
		});
		
		double [][] weights = new double[][] {
				{0.0, 1.0, 0.0, 0.0, 0.0, 0.0},
				{0.0, 0.0, 1.0, 1.0, 0.0, 0.0},
				{0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
				{0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
				{0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
				{0.0, 0.0, 0.0, 0.0, 0.0, 0.0}
		};
		
		int [][] refPaths = new int[][] {
				{4, 0, 5},
				{4, 1, 5},
				{4, 2, 5},
				{4, 3, 5},
				{4, 1, 2, 5},
				{4, 0, 1, 5},
				{4, 1, 3, 5},
				{4, 0, 1, 2, 5},
				{4, 0, 1, 3, 5}
		};
		
		double [] costs = {0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 1.0, 2.0, 2.0};
		
		runTest(idg, weights, refPaths, costs, 4, 5);
	}
	
	@Test
	public void testInstance_6_2() {
		IndexedNeighborGraph idg = LightweightStaticGraph.fromAdjacency(new int [][] {
				{1},
				{2, 3, 5},
				{5},
				{5},
				{0, 1, 3},
				{},
		});
		
		double [][] weights = new double[][] {
				{0.0, 1.0, 0.0, 0.0, 0.0, 0.0},
				{0.0, 0.0, 1.0, 1.0, 0.0, 0.0},
				{0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
				{0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
				{0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
				{0.0, 0.0, 0.0, 0.0, 0.0, 0.0}
		};
		
		int [][] refPaths = new int[][] {
				{4, 3, 5},
				{4, 1, 5},
				{4, 0, 1, 5},
				{4, 1, 2, 5},
				{4, 1, 3, 5},
				{4, 0, 1, 2, 5},
				{4, 0, 1, 3, 5}
		};
		
		double [] costs = {0.0, 0.0, 1.0, 1.0, 1.0, 2.0, 2.0};
		
		runTest(idg, weights, refPaths, costs, 4, 5);
	}

	private void runTest(IndexedNeighborGraph idg, double[][] weights,
			int[][] refPaths, double[] costs, int source, int target) {

		TopKShortest tpk = new TopKShortest(idg, weights);
		ArrayList<PathEntry> paths = tpk.topKShortest(source, target,
				Integer.MAX_VALUE);
		
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
