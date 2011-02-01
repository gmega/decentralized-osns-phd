package it.unitn.disi.test.framework;

import it.unitn.disi.graph.GraphProtocol;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;
import it.unitn.disi.utils.peersim.INodeRegistry;
import it.unitn.disi.utils.peersim.NodeRegistry;
import it.unitn.disi.utils.peersim.NodeRegistryInit;
import it.unitn.disi.utils.peersim.SNNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import junit.framework.Assert;

import org.easymock.EasyMock;

import peersim.core.GeneralNode;
import peersim.core.Linkable;
import peersim.core.Network;
import peersim.core.NetworkInitializer;
import peersim.core.Node;
import peersim.core.Protocol;

public class TestNetworkBuilder implements Iterable<Node> {

	private HashMap<Node, Integer> fNodes = new HashMap<Node, Integer>();

	private ArrayList<Node> fOrderedNodes = new ArrayList<Node>();

	private long fIds = 0;

	public TestNetworkBuilder() {
		NetworkInitializer.createNodeArray();
		Network.setCapacity(0);
	}

	public Iterator<Node> iterator() {
		return fOrderedNodes.iterator();
	}

	public List<Node> getNodes() {
		return Collections.unmodifiableList(fOrderedNodes);
	}

	public void done() {
		// Sanity check.
		Integer pid = null;
		for (Integer value : fNodes.values()) {
			if (pid == null) {
				pid = value;
			} else {
				Assert.assertEquals(value, pid);
			}
		}

		for (int i = 0; i < fOrderedNodes.size(); i++) {
			Node node = fOrderedNodes.get(i);
			EasyMock.expect(node.protocolSize()).andReturn(fNodes.get(node))
					.anyTimes();
			node.setIndex(i);
			EasyMock.expectLastCall().once();
		}

		EasyMock.replay(fNodes.keySet().toArray());
		NetworkInitializer.createNodeArray();

		for (Node node : fOrderedNodes) {
			Network.add(node);
		}
		
		// Inits the node registry.
		NodeRegistry.getInstance().clear();
		NodeRegistryInit init = new NodeRegistryInit(null);
		init.execute();
	}

	public Node baseNode() {
		Node node = EasyMock.createMock(SNNode.class);
		EasyMock.expect(node.getID()).andReturn(fIds++).anyTimes();
		EasyMock.expect(node.isUp()).andReturn(true).anyTimes();
		node.setFailState(GeneralNode.DEAD);
		EasyMock.expectLastCall().once();

		fNodes.put(node, 0);
		fOrderedNodes.add(node);
		return node;
	}

	public int addProtocol(Node node, Protocol protocol) {
		chkNode(node);
		int pid = fNodes.get(node);

		EasyMock.expect(node.getProtocol(pid)).andReturn(protocol).anyTimes();
		fNodes.put(node, pid + 1);
		return pid;
	}

	public Node[] addNodes(int size) {
		Node[] nodes = new Node[size];
		for (int i = 0; i < size; i++) {
			nodes[i] = baseNode();
		}

		return nodes;
	}

	public int assignCompleteLinkable() {
		int pid = -1;
		Protocol singleton = new CompleteLinkable();
		for (Node node : fOrderedNodes) {
			pid = addProtocol(node, singleton);
		}
		return pid;
	}

	public int assignLinkable(long[][] adjacencies) {
		int pid = -1;
		ConstrainedLinkableBuilder builder = new ConstrainedLinkableBuilder(
				adjacencies);
		for (int i = 0; i < fNodes.size(); i++) {
			Node node = fOrderedNodes.get(i);
			pid = addProtocol(fOrderedNodes.get(i),
					builder.linkableOf(node, i, NodeRegistry.getInstance()));
		}
		return pid;
	}

	private void chkNode(Node node) {
		if (!fNodes.containsKey(node)) {
			throw new IllegalArgumentException();
		}
	}

	class CompleteLinkable implements Linkable, Protocol {

		public boolean contains(Node neighbor) {
			return fNodes.containsKey(neighbor);
		}

		public int degree() {
			return fNodes.size();
		}

		public Node getNeighbor(int i) {
			return fOrderedNodes.get(i);
		}

		public boolean addNeighbor(Node neighbour) {
			throw new UnsupportedOperationException();
		}

		public Object clone() {
			return this;
		}

		public void pack() {
		}

		public void onKill() {
		}
	}

	class ConstrainedLinkableBuilder {
		LightweightStaticGraph fGraph;

		private ConstrainedLinkableBuilder(long[][] ids) {
			int[][] copy = new int[ids.length][];
			for (int i = 0; i < ids.length; i++) {
				copy[i] = new int[ids[i].length];
 				for (int j = 0; j < ids[i].length; j++) {
					copy[i][j] = (int) ids[i][j];
				}
			}

			fGraph = LightweightStaticGraph.fromAdjacency(copy);
		}

		private Protocol linkableOf(Node node, int id, INodeRegistry registry) {
			GraphProtocol protocol = new GraphProtocol();
			protocol.configure(id, fGraph, registry);
			return protocol;
		}

	}
}
