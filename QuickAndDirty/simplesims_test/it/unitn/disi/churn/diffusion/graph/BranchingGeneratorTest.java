package it.unitn.disi.churn.diffusion.graph;

import java.util.Arrays;

import junit.framework.Assert;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.analysis.PathEntry;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;
import it.unitn.disi.utils.collections.Pair;

import org.junit.Test;

public class BranchingGeneratorTest {

	@Test
	public void testBranchingGenerator() {
		int[][] original = { 
				{ 0, 6 }, 
				{ 0, 3, 6 }, 
				{ 0, 1, 3, 5, 6 },
				{ 0, 1, 2, 3, 4, 5, 6 } 
				};

		int[][] unfold = { 
				{ 1, 2, 3, 6 }, 
				{ 0, 2, 5, 10 }, 
				{ 0, 1 }, 
				{ 0, 4 },
				{ 3, 5 }, 
				{ 1, 4 }, 
				{ 0, 7 }, 
				{ 6, 8 }, 
				{ 7, 9 }, 
				{ 8, 10 },
				{ 1, 9 } };

		int[] refmap = { 0, 6, 3, 1, 3, 5, 1, 2, 3, 4, 5 };

		PathEntry[] entries = new PathEntry[original.length];
		for (int i = 0; i < entries.length; i++) {
			entries[i] = new PathEntry(original[i], 0);
		}

		Pair<IndexedNeighborGraph, int[]> result = BranchingGraphGenerator
				.branchingGraph(entries);

		LightweightStaticGraph graph = (LightweightStaticGraph) result.a;
		int[] map = result.b;

		Assert.assertTrue(Arrays.equals(map, refmap));
		Assert.assertEquals(unfold.length, graph.size());

		for (int i = 0; i < unfold.length; i++) {
			System.out.println(Arrays.toString(unfold[i]) + " == "
					+ Arrays.toString(graph.fastGetNeighbours(i)));
			Assert.assertTrue(Arrays.equals(unfold[i],
					graph.fastGetNeighbours(i)));
		}
	}
}
