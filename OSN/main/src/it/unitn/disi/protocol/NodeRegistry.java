package it.unitn.disi.protocol;

import java.util.HashMap;

import peersim.core.Node;

/**
 * {@link NodeRegistry} is a hack. It is simply an easily-accessible
 * {@link HashMap} used to reversely map node ids into their {@link Node}
 * objects.
 * 
 * @author giuliano
 */
public class NodeRegistry {
	private final HashMap<Object, Node> fId2Node = new HashMap<Object, Node>();

	private static final NodeRegistry fInstance = new NodeRegistry();

	public static NodeRegistry getInstance() {
		return fInstance;
	}

	private NodeRegistry() {
	}

	public void registerNode(Node node) {
		if (contains(node.getID())) {
			throw new RuntimeException("Duplicate key detected ("
					+ node.getID() + ").");
		}
		fId2Node.put(node.getID(), node);
	}

	public Node getNode(long id) {
		return fId2Node.get(id);
	}
	
	public Node removeNode(long id){
		return fId2Node.remove(id);
	}

	public boolean contains(long id) {
		return fId2Node.containsKey(id);
	}
}
