package it.unitn.disi.epidemics;

import it.unitn.disi.epidemics.ISelectionFilter;
import peersim.core.Node;

/**
 * Generic interface to a peer selection service.
 * 
 * @author giuliano
 */
public interface IPeerSelector extends ICachingObject {
	/**
	 * Selects a peer, constraining selection to nodes that match a selection
	 * criterion (for which {@link ISelectionFilter#canSelect(Node)} returns
	 * true. The method will call back {@link ISelectionFilter#selected(Node)}
	 * prior to returning the node.
	 * 
	 * @param source
	 *            selection will be performed on a neighborhood of this node.
	 * 
	 * @param filter
	 *            the selection criterion.
	 * 
	 * @return a neighbor satisfying the selection criterion.
	 * 
	 * @throws UnsupportedOperationException
	 *             if {@link #supportsFiltering()} returns false.
	 * 
	 * @see ISelectionFilter
	 */
	public Node selectPeer(Node source, ISelectionFilter filter);
}
