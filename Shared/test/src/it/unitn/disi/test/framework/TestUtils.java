package it.unitn.disi.test.framework;

import it.unitn.disi.cli.Adj2ByteGraph;
import it.unitn.disi.utils.graph.GraphWriter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.net.URL;

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
		try {
			Adj2ByteGraph conv = new Adj2ByteGraph();
			ByteArrayOutputStream ost = new ByteArrayOutputStream();
			conv.execute(new ByteArrayInputStream(graph.getBytes()), ost);
			return new ByteArrayInputStream(ost.toByteArray());
		}catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

}
