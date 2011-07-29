package it.unitn.disi.utils.peersim;

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
	 * @return whether this protocol has been initialized for the first time
	 *         already or not.
	 */
	public boolean isInitialized();

	/**
	 * Called whenever the node owning this protocol instance rejoins the
	 * network.
	 */
	public void reinitialize();
}
