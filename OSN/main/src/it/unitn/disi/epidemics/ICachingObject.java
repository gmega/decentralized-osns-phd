package it.unitn.disi.epidemics;

import peersim.core.Node;

/**
 * An {@link ICachingObject} holds an internal cache which uses up memory. 
 * 
 * @author giuliano
 */
public interface ICachingObject {
	/**
	 * Clears up the cache.
	 * 
	 * @param node
	 *            the {@link Node} object with which the caching object is
	 *            associated with.
	 */
	public void clear(Node source);
}
