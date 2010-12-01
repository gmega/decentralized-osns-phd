package it.unitn.disi.graph.cli;


import it.unitn.disi.graph.cli.ComputeConnectivity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
				{ 0, 6 } 
			}, new int[] {0, 1, 2, 3, 4, 5, 6, 7}, 3);
		
		runTarjanTest(new int[][] {
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
				{ 10 }
		}, new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12}, 6);
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
		Assert.assertEquals(6, create(graph).tarjan(new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12}));
	}
	
	private void runTarjanTest(int [][] graph, int [] subgraph, int expected) throws IOException{
		Assert.assertEquals(expected, create(graph).tarjan(subgraph));
	}
	
	private ComputeConnectivity create(int[][] graph) throws IOException{
		ComputeConnectivity computer = new ComputeConnectivity();
		byte[] encGraph = encode(graph);
		computer.load(new ByteArrayInputStream(encGraph));
		computer.initTarjan();
		return computer;
	}

	private byte[] encode(int[][] graph) throws IOException {
		ByteArrayOutputStream oup = new ByteArrayOutputStream();
		byte[] buffer = new byte[4];
		for (int i = 0; i < graph.length; i++) {
			for (int j = 0; j < graph[i].length; j++) {
				oup.write(toBytes(i, buffer));
				oup.write(toBytes(graph[i][j], buffer));
			}
		}

		return oup.toByteArray();
	}

	private byte[] toBytes(int num, byte[] buf) {
		buf[3] = (byte) ((num & 0xff000000) >>> 24);
		buf[2] = (byte) ((num & 0x00ff0000) >>> 16);
		buf[1] = (byte) ((num & 0x0000ff00) >>> 8);
		buf[0] = (byte) ((num & 0x000000ff));
		return buf;
	}

}
