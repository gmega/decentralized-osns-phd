package it.unitn.disi.sps;

import peersim.core.Node;

public class NodeEntry {
	
	private final Node fNode;
	
	private int fAge;

	public NodeEntry(Node node) {
		this(node, 0);
	}
	
	public NodeEntry(Node node, int age) {
		this.fNode = node;
		this.fAge = age;
	}
	
	public Node node() {
		return fNode;
	}
	
	public int age() {
		return fAge;
	}
	
	public void increaseAge() {
		fAge++;
	}
}
