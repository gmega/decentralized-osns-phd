package it.unitn.disi.graph.cli;

import gnu.trove.list.array.TIntArrayList;
import it.unitn.disi.graph.algorithms.GraphAlgorithms;
import it.unitn.disi.graph.algorithms.GraphAlgorithms.TarjanState;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;

import java.io.IOException;

import junit.framework.Assert;

import org.junit.Test;

public class TestConnectivityComputer {

	@Test
	public void testBasicTarjan() throws IOException {
		runTarjanTest(new int[][] { 
				{ 1 }, 
				{ 2, 7 }, 
				{ 3, 6 }, 
				{ 4 }, 
				{ 2, 5 },
				{}, 
				{ 3, 5 }, 
				{ 0, 6 } }, new int[] { 0, 1, 2, 3, 4, 5, 6, 7 }, 3);

		runTarjanTest(
				new int[][] { 
						{ 1, 2 }, 
						{ 2 }, 
						{ 3 }, 
						{ 0, 4 },
						{ 5, 6 },
						{ 6, 10 }, 
						{ 7 }, 
						{ 8 }, 
						{ 9 }, 
						{ 7 }, 
						{ 11 }, 
						{ 12 },
						{ 10 } },
						new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
						11, 12 }, 6);
	}

	@Test
	public void testTarjanWithSubgraphs() throws IOException {
		int [][]graph = new int[][] {
				{ 1, 2, 13},
				{ 2, 13 },
				{ 3, 13 },
				{ 0, 4, 13 },
				{ 5, 6, 13 },
				{ 6, 10, 13 }, 
				{ 7, 13 },
				{ 8, 13 },
				{ 9, 13 },
				{ 7, 13 },
				{ 11, 13 },
				{ 12, 13 },
				{ 10, 13 },
				{ 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 }
			};	
		
		runTarjanTest(graph, new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13}, 1);
		runTarjanTest(graph, new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12}, 6);
	}
	
	private void runTarjanTest(int[][] graphAdj, int[] subgraphAdj, int expected)
			throws IOException {

		TarjanState state = new TarjanState();
		state.ensureSize(subgraphAdj.length);
		LightweightStaticGraph graph = LightweightStaticGraph
				.fromAdjacency(graphAdj);
		
		LightweightStaticGraph subgraph = LightweightStaticGraph
				.subgraph(graph, subgraphAdj); 

		Assert.assertEquals(expected, GraphAlgorithms.tarjan(state, subgraph));
		
		int i = 0;
		for (TIntArrayList component : state.components) {
			i++;
			System.err.println("C" + i + ": " + component);
		}

	}

}
