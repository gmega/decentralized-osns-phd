package it.unitn.disi.utils;

import peersim.core.Linkable;
import peersim.core.Node;
import peersim.core.Protocol;

/**
 * An {@link IReference} knows how to find an object (usually a {@link Protocol}
 * or {@link Linkable}) that belongs to a {@link Node}.
 * 
 * @author giuliano
 */
public interface IReference<K> {
	/**
	 * @parameter the object owner.
	 * 
	 * @return the referred object.
	 */
	public K get(Node owner);
}
