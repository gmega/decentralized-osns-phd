package it.unitn.disi;

import it.unitn.disi.cli.Adj2ByteGraph;
import it.unitn.disi.codecs.AdjListGraphDecoder;
import it.unitn.disi.utils.graph.GraphWriter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;

import org.easymock.EasyMock;

import peersim.core.Linkable;
import peersim.core.Node;
import peersim.core.Protocol;
import peersim.graph.Graph;

public class TestUtils {
	private static Set<Node> allNodes = new HashSet<Node>();
	
	public static void reset() {
		allNodes.clear();
	}
	
	public static void replayAll() {
		EasyMock.replay(allNodes.toArray());
	}
	
	public static Node makeNode(){ 
		Node node = baseNode();
		EasyMock.replay(node);
		return node;
	}

	public static Node baseNode() {
		Node node = EasyMock.createMock(Node.class);
		EasyMock.expect(node.isUp()).andReturn(true).anyTimes();
		allNodes.add(node);
		return node;
	}
	
	public static Node addProtocol(Node node, int i, Protocol protocol) {
		EasyMock.expect(node.getProtocol(i)).andReturn(protocol).anyTimes();
		
		return node;
	}
	
	public static Node[] mkNodeArray(int size) {
		Node [] nodes = new Node[size];
		for (int i = 0; i < size; i++) {
			nodes[i] = makeNode();
		}
		
		return nodes;
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
	
	public static Linkable allSocialNetwork() {
		return socialNetwork(allNodes);
	}
	
	public static Linkable socialNetwork(final Set<Node> pertains) {
		return new Linkable() {

			public boolean contains(Node neighbor) {
				return pertains.contains(neighbor);
			}

			public int degree() {
				throw new UnsupportedOperationException();
			}

			public Node getNeighbor(int i) {
				throw new UnsupportedOperationException();
			}
			
			public boolean addNeighbor(Node neighbour) {
				throw new UnsupportedOperationException();
			}

			public void pack() {
			}

			public void onKill() {
			}
			
		};
	}
}
