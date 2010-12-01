package it.unitn.disi.test.framework;

import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.graph.GraphWriter;
import it.unitn.disi.graph.Undirect;
import it.unitn.disi.graph.cli.Adj2ByteGraph;
import it.unitn.disi.graph.codecs.AdjListGraphDecoder;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.net.URL;

import junit.framework.Assert;

import org.easymock.EasyMock;

import peersim.core.Linkable;
import peersim.core.Node;
import peersim.graph.Graph;

public class TestUtils {
	
	public static URL locate(String name) {
		return TestUtils.class.getClassLoader().getResource("creator_test_config.properties");
	}
	
	public static Linkable completeSocialNetwork() {
		Linkable sn = EasyMock.createMock(Linkable.class);
		EasyMock.expect(sn.contains((Node)EasyMock.anyObject())).andReturn(true).anyTimes();
		EasyMock.replay(sn);
		return sn;
	}
	
	public static ByteArrayInputStream blob(Graph g) {
		StringWriter writer = new StringWriter();
		GraphWriter.printAdjList(g, writer);
		return encode(writer.getBuffer().toString());
	}
	
	public static ByteArrayInputStream encode(String graph) {
		return runTransformer(new Adj2ByteGraph(), new ByteArrayInputStream(graph.getBytes()));
	}
	
	public static ByteArrayInputStream undirect(ByteArrayInputStream input) {
		return runTransformer(new Undirect(), input);
	}
	
	public static LightweightStaticGraph graph(String adjList) {
		try {
			AdjListGraphDecoder decoder = new AdjListGraphDecoder(new ByteArrayInputStream(adjList.getBytes()));
			return LightweightStaticGraph.load(decoder);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
		
	public static ByteArrayInputStream runTransformer(ITransformer transformer, ByteArrayInputStream input) {
		try {
			ByteArrayOutputStream ost = new ByteArrayOutputStream();
			transformer.execute(input, ost);
			return new ByteArrayInputStream(ost.toByteArray());
		}catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	public static <T> void assertContains(T element, T [] array) {
		for (int i = 0; i < array.length; i++) {
			if (element.equals(array[i])) {
				return;
			}
		}
		
		Assert.fail("Element " + element.toString() + " not found.");
	}

}
