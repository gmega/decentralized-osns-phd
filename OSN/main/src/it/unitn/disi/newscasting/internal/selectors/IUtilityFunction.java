package it.unitn.disi.newscasting.internal.selectors;

import peersim.core.Node;
import peersim.core.Protocol;

/**
 * A {@link IUtilityFunction} can compute the utility of one node relative to
 * another.
 * 
 * @author giuliano
 */
public interface IUtilityFunction extends Protocol {
	/**
	 * @return the relative utility of <code>target</code> with respect to
	 *         <code>base</code>. The value should be equal or larger than zero.
	 */
	public int utility(Node base, Node target);

	/**
	 * Tells whether or not the values for this utility function vary in time.
	 */
	public boolean isDynamic();
}
