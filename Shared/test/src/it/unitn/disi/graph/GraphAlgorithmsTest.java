package it.unitn.disi.graph;
import it.unitn.disi.graph.analysis.GraphAlgorithms;
import it.unitn.disi.graph.codecs.ByteGraphDecoder;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;
import it.unitn.disi.test.framework.TestUtils;

import java.io.InputStream;

import junit.framework.Assert;

import org.junit.Test;

public class GraphAlgorithmsTest {
	@Test
	public void testIsConnected() throws Exception {
		InputStream disconn = TestUtils.encode(
				"0 1 2\n" +
				"1 2 3\n" +
				"3 4 5\n" +
				"6 7 8");
		
		InputStream conn = TestUtils.encode(
				"0 1 2\n" +
				"1 2 3\n" +
				"3 4 5\n" +
				"6 7 8\n" + 
				"5 6");
		
		LightweightStaticGraph disconnG = LightweightStaticGraph
				.load(new ByteGraphDecoder(disconn));
		LightweightStaticGraph connG = LightweightStaticGraph
				.load(new ByteGraphDecoder(conn));		
		
		Assert.assertFalse(GraphAlgorithms.isConnected(disconnG));
		Assert.assertTrue(GraphAlgorithms.isConnected(connG));
	}
}
