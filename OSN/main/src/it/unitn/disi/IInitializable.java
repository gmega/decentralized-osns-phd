package it.unitn.disi;

import peersim.core.Node;

/**
 * Generic interface for initialization and re-initialization of protocols.
 * 
 * @author giuliano
 */
public interface IInitializable {
	/**
	 * Called when the protocol is first initialized.
	 * 
	 * @param node
	 *            the node owning this protocol instance.
	 */
	public void initialize(Node node);

	/**
	 * Called whenever the node owning this protocol instance rejoins the
	 * network.
	 */
	public void reinitialize();
}
