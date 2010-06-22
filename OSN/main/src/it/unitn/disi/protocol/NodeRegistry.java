package it.unitn.disi.protocol;

import it.unitn.disi.utils.MiscUtils;

import java.util.ArrayList;
import java.util.HashMap;

import peersim.config.Configuration;
import peersim.core.Network;
import peersim.core.Node;

public class NodeRegistry {
	
	private static final String PAR_NODE_REGISTRY = "it.unitn.disi.registry";
	
	private static final INodeRegistry fInstance;
	
	static {
		if(Configuration.getString(PAR_NODE_REGISTRY).equals("contiguous")){
			System.err.println("Using contiguous node registry.");
			fInstance = new ArrayListNodeRegistry(Network.size());
		} else {
			fInstance = new HashMapNodeRegistry();
		}
	}
	
	public static INodeRegistry getInstance() {
		return fInstance;
	}
	
	private NodeRegistry(){ }
}

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

class HashMapNodeRegistry extends AbstractNodeRegistry {
	
	private final HashMap<Object, Node> fId2Node = new HashMap<Object, Node>();
	HashMapNodeRegistry() { }

	protected void store(long id, Node node) { fId2Node.put(id, node); }
	public Node getNode(long id) { return fId2Node.get(id); }
	public Node removeNode(long id){ return fId2Node.remove(id); }
	public boolean contains(long id) { return fId2Node.containsKey(id); }
}


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


