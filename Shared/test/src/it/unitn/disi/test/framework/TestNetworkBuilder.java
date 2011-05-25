package it.unitn.disi.test.framework;

import it.unitn.disi.graph.GraphProtocol;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;
import it.unitn.disi.utils.peersim.INodeRegistry;
import it.unitn.disi.utils.peersim.NodeRegistry;
import it.unitn.disi.utils.peersim.NodeRegistryInit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import junit.framework.Assert;

import peersim.core.Linkable;
import peersim.core.Network;
import peersim.core.NetworkInitializer;
import peersim.core.Node;
import peersim.core.Protocol;

public class TestNetworkBuilder implements Iterable<TestNodeImpl> {

	private ArrayList<TestNodeImpl> fOrderedNodes = new ArrayList<TestNodeImpl>();

	public TestNetworkBuilder() {
		NetworkInitializer.createNodeArray();
		TestNodeImpl.resetIDCounter();
		Network.setCapacity(0);
	}

	public Iterator<TestNodeImpl> iterator() {
		return (Iterator<TestNodeImpl>) fOrderedNodes.iterator();
	}

	public List<? extends Node> getNodes() {
		return Collections.unmodifiableList(fOrderedNodes);
	}

	public int size() {
		return fOrderedNodes.size();
	}

	public void done() {
		// Sanity check.
		Integer pid = null;
		for (Node node : fOrderedNodes) {
			if (pid == null) {
				pid = node.protocolSize();
			} else {
				Assert.assertEquals(node.protocolSize(), pid.intValue());
			}
		}
		
		// Applies config to PeerSim.
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
		TestNodeImpl node = new TestNodeImpl();
		fOrderedNodes.add(node);
		return node;
	}

	public int addProtocol(Node node, Protocol protocol) {
		return chkNode(node).addProtocol(protocol);
	}
	
	public int nextProtocolId(Node node) {
		return chkNode(node).protocolSize();
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
		for (int i = 0; i < fOrderedNodes.size(); i++) {
			Node node = fOrderedNodes.get(i);
			pid = addProtocol(fOrderedNodes.get(i),
					builder.linkableOf(node, i, NodeRegistry.getInstance()));
		}
		return pid;
	}

	private TestNodeImpl chkNode(Node node) {
		if (!fOrderedNodes.contains(node)) {
			throw new IllegalArgumentException();
		}
		return (TestNodeImpl) node;
	}

	class CompleteLinkable implements Linkable, Protocol {

		public boolean contains(Node neighbor) {
			return fOrderedNodes.contains(neighbor);
		}

		public int degree() {
			return fOrderedNodes.size();
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
