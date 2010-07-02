package it.unitn.disi.utils.peersim;

import peersim.core.Node;

/**
 * {@link INodeRegistry} is a hack. It is simply an easily-accessible map used
 * to reversely retrieve {@link Node} objects by their ids.
 * 
 * @author giuliano
 */
public interface INodeRegistry {

	/**
	 * Register a node.
	 */
	public void registerNode(Node node);

	/**
	 * Retrieves a node by ID.
	 * 
	 * @param id
	 *            a node ID.
	 * @return the node with corresponding ID, or <code>null</code> if there is
	 *         none.
	 */
	public Node getNode(long id);

	/**
	 * Removes a node by ID.
	 * 
	 * @param id
	 *            id of the node to be removed.
	 * @return the removed node, or <code>null</code> if the ID does not exist.
	 */
	public Node removeNode(long id);

	/**
	 * @return <code>true</code> if a node with the provided ID exists, or
	 *         <code>false</code> otherwise.
	 */
	public boolean contains(long id);

}