package it.unitn.disi.sps.cyclon;

import peersim.core.Node;

public class NodeDescriptor implements Comparable<NodeDescriptor>{
	
	public static NodeDescriptor cloneFrom(NodeDescriptor descriptor) {
		return new NodeDescriptor(descriptor.node(), descriptor.age());
	}
	
	private final Node fNode;
	
	private int fAge;
	
	public NodeDescriptor(Node node) {
		this(node,0);
	}
	
	public NodeDescriptor(Node node, int age) {
		fNode = node;
		fAge = age;
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
	
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof NodeDescriptor)) {
			return false;
		}
		
		NodeDescriptor descriptor = (NodeDescriptor) other;
		return this.fNode.equals(descriptor.node())
				&& this.age() == descriptor.age();
	}

	@Override
	public int hashCode() {
		int hashCode = 101;
		hashCode += 37*fNode.hashCode();
		hashCode += 37*fAge;
		return hashCode;
	}

	@Override
	public int compareTo(NodeDescriptor o) {
		return this.age() - o.age();
	}
	
}
