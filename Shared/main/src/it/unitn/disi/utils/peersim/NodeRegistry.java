package it.unitn.disi.utils.peersim;


import it.unitn.disi.utils.MiscUtils;

import java.util.ArrayList;
import java.util.HashMap;

import peersim.config.Configuration;
import peersim.core.Network;
import peersim.core.Node;

/**
 * {@link NodeRegistry} provides access to an {@link INodeRegistry} singleton
 * (for lack of a better configuration infrastructure).
 * 
 * @author giuliano
 * @see INodeRegistry
 */
public class NodeRegistry {
	
	/**
	 * Node registry type.
	 */
	private static final String PAR_NODE_REGISTRY = "it.unitn.disi.registry";

	/**
	 * Setting {@link #PAR_NODE_REGISTRY} to this value will cause an array to
	 * be used for mapping. If the ID range for the nodes is contiguous, this 
	 * will provide significant performance gains.
	 */
	
	private static final INodeRegistry fInstance;
	
	/** Configures the {@link NodeRegistry} instance. **/
	static {
		String mode = null;
		try {
			mode = Configuration.getString(PAR_NODE_REGISTRY, "hash");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		if("contiguous".equals(mode)){
			System.err.println("Using contiguous node registry.");
			System.err.print("Allocating... ");
			fInstance = new ArrayListNodeRegistry(Network.size());
			System.err.println("done.");
		} else {
			fInstance = new HashMapNodeRegistry();
		}
	}
	
	/**
	 * @return the singleton instance.
	 */
	public static INodeRegistry getInstance() {
		return fInstance;
	}
	
	private NodeRegistry(){ }
}

/**
 * {@link AbstractNodeRegistry} is the base class for a node registry. Node
 * registries are global maps providing <i>O(1)</i> access to {@link Node}
 * objects from their IDs.
 */
abstract class AbstractNodeRegistry implements INodeRegistry {
	
	/* (non-Javadoc)
	 * @see it.unitn.disi.protocol.INodeRegistry#registerNode(peersim.core.Node)
	 */
	public void registerNode(Node node) {
		if (contains(node.getID())) {
			throw new RuntimeException("Duplicate key detected ("
					+ node.getID() + ").");
		}
		store(node.getID(), node);
	}

	protected abstract void store(long id, Node node);
}

/**
 * {@link INodeRegistry} backed by a {@link HashMap}. Efficient if the ID space
 * is much larger than the number of nodes in the network.
 * 
 * @author giuliano
 * 
 */
class HashMapNodeRegistry extends AbstractNodeRegistry {
	
	private final HashMap<Object, Node> fId2Node = new HashMap<Object, Node>();
	HashMapNodeRegistry() { }

	protected void store(long id, Node node) { fId2Node.put(id, node); }
	public Node getNode(long id) { return fId2Node.get(id); }
	public Node removeNode(long id){ return fId2Node.remove(id); }
	public boolean contains(long id) { return fId2Node.containsKey(id); }
}

/**
 * {@link INodeRegistry} backed by an array. Efficient if the ID space is
 * roughly the same size as the number of nodes in the network.
 * <BR><BR>
 * Note: if used with large ID spaces, this implementation will quickly 
 * cause the virtual machine to run out of memory.
 * 
 * @author giuliano
 */
class ArrayListNodeRegistry extends AbstractNodeRegistry {
	private final ArrayList<Node> fId2Node;
	ArrayListNodeRegistry(int size) { 
		fId2Node = new ArrayList<Node>(size);
	}

	protected void store(long id, Node node) {
		int int_id = (int)id;
		MiscUtils.grow(fId2Node, int_id+1);
		fId2Node.set(int_id, node);
	}
	
	public Node getNode(long id) {
		if (id >= fId2Node.size()) {
			return null;
		}
		
		return fId2Node.get((int)id); 
	}
	
	public Node removeNode(long id){
		if (id >= fId2Node.size()) {
			return null;
		}
		
		return fId2Node.set((int)id, null); 
	}
	
	public boolean contains(long id) {
		if (id >= fId2Node.size()) {
			return false;
		}
		
		return fId2Node.get((int) id) != null; 
	}

}


