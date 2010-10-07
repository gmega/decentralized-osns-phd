package it.unitn.disi.sps;

import peersim.core.Linkable;
import peersim.core.Node;

public interface IPeerSamplingLinkable extends Linkable {
	/**
	 * Returns the age (from the perspective of the underlying peer sampling
	 * service) of a {@link Node} in the current view.
	 * 
	 * @param i
	 *            the index of the node in the current view.
	 * 
	 * @throws ArrayIndexOutOfBoundsException
	 *             if i > {@link #degree()}.
	 */
	public int getAge(int i);
}
